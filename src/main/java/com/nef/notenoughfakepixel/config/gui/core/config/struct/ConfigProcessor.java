/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.config.gui.core.config.struct;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.*;
import com.nef.notenoughfakepixel.config.gui.core.config.gui.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigProcessor {

    public static class ProcessedCategory {

        public final String name;
        public final String desc;
        public final LinkedHashMap<String, ProcessedOption> options = new LinkedHashMap<>();
        
        public final List<String> aliases = new ArrayList<>();

        public ProcessedCategory(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }
    }

    public static class ProcessedOption {

        public final String name;
        public final String desc;
        public final int subcategoryId;
        public GuiOptionEditor editor;

        public int accordionId = -1;

        private final Field field;
        private final Object container;

        public ProcessedOption(String name, String desc, int subcategoryId, Field field, Object container) {
            this.name = name;
            this.desc = desc;
            this.subcategoryId = subcategoryId;

            this.field = field;
            this.container = container;
        }

        public Object get() {
            try {
                return field.get(container);
            } catch (Exception e) {
                return null;
            }
        }

        public boolean set(Object value) {
            try {
                if (field.getType() == int.class && value instanceof Number) {
                    field.set(container, ((Number) value).intValue());
                } else {
                    field.set(container, value);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static final class SectionToggle {
        public boolean open = false;
    }

    private static final int SECTION_ID_BLOCK = 1000;

    public static LinkedHashMap<String, ProcessedCategory> create(Configuration config) {
        LinkedHashMap<String, ProcessedCategory> processedConfig = new LinkedHashMap<>();
        
        Map<String, ProcessedCategory> byName = new HashMap<>();
        Map<ProcessedCategory, Integer> sectionCount = new HashMap<>();

        for (Field categoryField : config.getClass().getDeclaredFields()) {
            boolean exposePresent = categoryField.isAnnotationPresent(Expose.class);
            boolean categoryPresent = categoryField.isAnnotationPresent(Category.class);

            if (exposePresent && categoryPresent) {
                Object categoryObj;
                try {
                    categoryObj = categoryField.get(config);
                } catch (Exception e) {
                    
                    continue;
                }

                Category categoryAnnotation = categoryField.getAnnotation(Category.class);
                String nameKey = categoryAnnotation.name().toLowerCase();
                ProcessedCategory cat = byName.get(nameKey);
                if (cat == null) {
                    cat = new ProcessedCategory(categoryAnnotation.name(), categoryAnnotation.desc());
                    byName.put(nameKey, cat);
                    processedConfig.put(categoryField.getName(), cat);
                }

                String section = categoryAnnotation.section();
                if (section.isEmpty()) {
                    addOptions(cat.options, categoryObj, 0, -1, config);
                } else {
                    int index = sectionCount.merge(cat, 1, Integer::sum);
                    int idOffset = index * SECTION_ID_BLOCK;
                    int wrapperId = idOffset + SECTION_ID_BLOCK - 1;

                    LinkedHashMap<String, ProcessedOption> sectionOptions = new LinkedHashMap<>();
                    addOptions(sectionOptions, categoryObj, idOffset, wrapperId, config);
                    if (sectionOptions.isEmpty()) continue;

                    cat.aliases.add(section);
                    try {
                        Field openField = SectionToggle.class.getField("open");
                        ProcessedOption header = new ProcessedOption(section, categoryAnnotation.desc(), 0, openField, new SectionToggle());
                        header.editor = new GuiOptionEditorAccordion(header, wrapperId);
                        cat.options.put(uniqueKey(cat.options, categoryField.getName() + ".section"), header);
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                        continue;
                    }
                    for (Map.Entry<String, ProcessedOption> entry : sectionOptions.entrySet()) {
                        cat.options.put(uniqueKey(cat.options, entry.getKey()), entry.getValue());
                    }
                }
            } else if (exposePresent || categoryPresent) {
                
            }
        }
        return processedConfig;
    }

    private static String uniqueKey(Map<String, ProcessedOption> existing, String key) {
        String candidate = key;
        int n = 2;
        while (existing.containsKey(candidate)) candidate = key + "#" + n++;
        return candidate;
    }

    private static void addOptions(Map<String, ProcessedOption> target, Object categoryObj,
                                   int idOffset, int wrapperId, Configuration config) {
        for (Field optionField : categoryObj.getClass().getDeclaredFields()) {
            boolean optionPresent = optionField.isAnnotationPresent(ConfigOption.class);

            if (optionPresent) {
                ConfigOption optionAnnotation = optionField.getAnnotation(ConfigOption.class);
                ProcessedOption option = new ProcessedOption(optionAnnotation.name(), optionAnnotation.desc(), optionAnnotation.subcategoryId(), optionField, categoryObj);
                if (optionField.isAnnotationPresent(ConfigAccordionId.class)) {
                    ConfigAccordionId annotation = optionField.getAnnotation(ConfigAccordionId.class);
                    option.accordionId = annotation.id() + idOffset;
                } else {
                    option.accordionId = wrapperId;
                }

                GuiOptionEditor editor = null;
                Class<?> optionType = optionField.getType();
                if (optionType.isAssignableFrom(int.class) && optionField.isAnnotationPresent(ConfigEditorKeybind.class)) {
                    ConfigEditorKeybind configEditorAnnotation = optionField.getAnnotation(ConfigEditorKeybind.class);
                    editor = new GuiOptionEditorKeybind(option, (int) option.get(), configEditorAnnotation.defaultKey());
                }
                if (optionField.isAnnotationPresent(ConfigEditorButton.class)) {
                    ConfigEditorButton configEditorAnnotation = optionField.getAnnotation(ConfigEditorButton.class);
                    editor = new GuiOptionEditorButton(option, configEditorAnnotation.runnableId(), configEditorAnnotation.buttonText(), config);
                }
                if (optionType.isAssignableFrom(boolean.class) && optionField.isAnnotationPresent(ConfigEditorBoolean.class)) {
                    editor = new GuiOptionEditorBoolean(option);
                }
                if (optionType.isAssignableFrom(boolean.class) && optionField.isAnnotationPresent(ConfigEditorAccordion.class)) {
                    ConfigEditorAccordion configEditorAnnotation = optionField.getAnnotation(ConfigEditorAccordion.class);
                    editor = new GuiOptionEditorAccordion(option, configEditorAnnotation.id() + idOffset);
                }
                if (optionType.isAssignableFrom(int.class)) {
                    if (optionField.isAnnotationPresent(ConfigEditorDropdown.class)) {
                        ConfigEditorDropdown configEditorAnnotation = optionField.getAnnotation(ConfigEditorDropdown.class);
                        editor = new GuiOptionEditorDropdown(option, configEditorAnnotation.values(), (int) option.get(), true);
                    } else if (optionField.isAnnotationPresent(ConfigEditorStyle.class)) {
                        editor = new GuiOptionEditorStyle(option, (int) option.get());
                    }
                }
                if (optionType.isAssignableFrom(List.class)) {
                    if (optionField.isAnnotationPresent(ConfigEditorDraggableList.class)) {
                        ConfigEditorDraggableList configEditorAnnotation = optionField.getAnnotation(ConfigEditorDraggableList.class);
                        editor = new GuiOptionEditorDraggableList(option, configEditorAnnotation.exampleText());
                    }
                }
                if (optionType.isAssignableFrom(String.class)) {
                    if (optionField.isAnnotationPresent(ConfigEditorDropdown.class)) {
                        ConfigEditorDropdown configEditorAnnotation = optionField.getAnnotation(ConfigEditorDropdown.class);
                        editor = new GuiOptionEditorDropdown(option, configEditorAnnotation.values(), configEditorAnnotation.initialIndex(), false);
                    } else if (optionField.isAnnotationPresent(ConfigEditorColour.class)) {
                        editor = new GuiOptionEditorColour(option);
                    } else if (optionField.isAnnotationPresent(ConfigEditorText.class)) {
                        editor = new GuiOptionEditorText(option);
                    }
                }
                if (optionType.isAssignableFrom(int.class) || optionType.isAssignableFrom(float.class) || optionType.isAssignableFrom(double.class)) {
                    if (optionField.isAnnotationPresent(ConfigEditorSlider.class)) {
                        ConfigEditorSlider configEditorAnnotation = optionField.getAnnotation(ConfigEditorSlider.class);
                        editor = new GuiOptionEditorSlider(option, configEditorAnnotation.minValue(), configEditorAnnotation.maxValue(), configEditorAnnotation.minStep());
                    }
                }
                if (optionType.isAssignableFrom(String.class)) {
                    if (optionField.isAnnotationPresent(ConfigEditorDropdown.class)) {
                        ConfigEditorDropdown configEditorAnnotation = optionField.getAnnotation(ConfigEditorDropdown.class);
                        editor = new GuiOptionEditorDropdown(option, configEditorAnnotation.values(), 0, false);
                    }
                }
                if (editor == null) {
                    
                    continue;
                }
                option.editor = editor;
                target.put(uniqueKey(target, optionField.getName()), option);
            }
        }
    }
}
