package csv;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Omar Muhtaseb
 * @since 2018-12-30
 */
public class CSVGenerator<T> {


    /**
     * Args Constructor
     *
     * @param clazz: The class of
     */
    public CSVGenerator(Class clazz) {
        this.clazz = clazz;
    }

    private Class clazz;
    private Map<Field, Set<Object>> mapOfMaps = new HashMap<>();
    private List<Field> fieldList;
    private boolean dataScanNeeded = false;
    private String header;
    private String data;
    private List<Field> unorderedMapFieldList = new ArrayList<>();
    private HashSet<Object> orderedMapKeys = new HashSet<>();
    private HashMap<Field, String[]> orderedMapFieldHashMap = new HashMap<>();

    /**
     * This method is used to generate the CSV text using the `clazz` as a mapper and dataList as values
     *
     * @param dataList: The data of the csv text
     */
    public String generateCSV(List<T> dataList) throws CSVException {
        constructCSVStructure(dataList);
        generateHeader();
        generateData(dataList);
        return header + data;
    }


    private void constructCSVStructure(List<T> dataList) throws CSVException {
        // Filter fields with CSVIgnore annotation
        List<Field> allFields = new ArrayList<>();
        getAllFields(allFields, clazz);
        fieldList = allFields.stream().filter(
                field -> !field.isAnnotationPresent(CSVIgnore.class))
                .collect(Collectors.toList());

        // Check if there is any map field with no `CSVMapOrderedKeys` defined
        fieldList.stream().filter(field -> Map.class.isAssignableFrom(field.getType())).forEach(field -> {
            CSVMapOrderedKeys CSVMapOrderedKeys = field.getAnnotation(CSVMapOrderedKeys.class);
            if (CSVMapOrderedKeys != null) {
                // Check whether to includeNull values or not
                if ((CSVMapOrderedKeys.includeNull())) {
                    mapOfMaps.put(field, new LinkedHashSet<>(Arrays.asList(CSVMapOrderedKeys.keys())));
                } else {
                    dataScanNeeded = true;
                    // Map between Field of type java.lang.Map and its values
                    orderedMapFieldHashMap.put(field, CSVMapOrderedKeys.keys());
                }
            } else {
                dataScanNeeded = true;
                unorderedMapFieldList.add(field);
            }
        });

        // In case the clazz has Fields of type java.lang.Map and `CSVMapOrderedKeys` annotation is not used
        // Or `CSVMapOrderedKeys` annotation is used with but with includeNull = false
        // then data will be traversed in order to get all the possible values for these columns
        if (dataScanNeeded) {
            dataList.forEach(throwingConsumerWrapper(this::scanData));

            // Sort the columns for all possible columns of maps with CSVMapOrderedKeys annotation and includeNull = false
            orderedMapFieldHashMap.keySet().forEach(key -> {
                LinkedHashSet<Object> tmpSet = Arrays.stream(orderedMapFieldHashMap.get(key))
                        .filter(orderedMapKeys::contains).collect(Collectors.toCollection(LinkedHashSet::new));
                mapOfMaps.put(key, tmpSet);
            });
        }
    }

    private void scanData(T data) throws CSVException {
        // Get header for maps with no CSVMapOrderedKeys annotation
        // List of Fields with of type java.lang.Map and with `CSVMapOrderedKeys` annotation not set
        unorderedMapFieldList.forEach(throwingConsumerWrapper(classMap -> {
            classMap.setAccessible(true);
            // get the map of the object
            if (classMap.get(data) != null) {
                Map<Object, Object> tmpMap = (Map<Object, Object>) classMap.get(data);
                if (tmpMap != null) {
                    Set<Object> currentClassMap = mapOfMaps.get(classMap);
                    if (currentClassMap == null) {
                        currentClassMap = new HashSet<>();
                    }
                    currentClassMap.addAll(tmpMap.keySet());
                    mapOfMaps.put(classMap, currentClassMap);
                }
                classMap.setAccessible(false);
            }
        }));

        // Get all possible columns for maps with CSVMapOrderedKeys annotation and includeNull = false
        // Set of Fields with of type java.lang.Map and with `CSVMapOrderedKeys` annotation set with includeNull = false
        orderedMapFieldHashMap.keySet().forEach(throwingConsumerWrapper(classMap -> {
            classMap.setAccessible(true);
            // get the map of the object
            if (classMap.get(data) != null) {
                Map<Object, Object> tmpMap = (Map<Object, Object>) classMap.get(data);
                if (tmpMap != null) {
                    orderedMapKeys.addAll(tmpMap.keySet());
                }
            }
            classMap.setAccessible(false);
        }));
    }

    private void generateHeader() throws CSVException {
        // Generate the header using the clazz fields and the keys of the maps
        List<Object> columnNames = new ArrayList<>();
        fieldList.forEach(field -> {
            // In case of map, get all the keys of the map
            if (Map.class.isAssignableFrom(field.getType())) {
                Set<Object> mapSet = mapOfMaps.get(field);
                if (mapSet != null) {
                    columnNames.addAll(mapSet);
                }
            } else {
                // Check if `CSVName` is set, otherwise use the field name
                CSVName csvName = field.getDeclaredAnnotation(CSVName.class);
                if (csvName != null) {
                    columnNames.add(csvName.value());
                } else {
                    columnNames.add(field.getName());
                }
            }
        });
        header = columnNames.stream().map(object -> "\"" + object.toString() + "\"").collect(Collectors.joining(",", "", "\n"));
    }

    private void generateData(List<T> dataList) throws CSVException {
        StringBuffer stringBuffer = new StringBuffer();
        dataList.forEach(throwingConsumerWrapper(data -> {
            List<Object> values = new ArrayList<>();
            fieldList.forEach(throwingConsumerWrapper(field -> {
                field.setAccessible(true);
                if (Map.class.isAssignableFrom(field.getType())) {
                    Map<Object, Object> tmpMap = (field.get(data) == null) ? new HashMap<>() :
                            (Map<Object, Object>) field.get(data);
                    mapOfMaps.get(field).forEach(key -> {
                        Object val = tmpMap.get(key);
                        values.add((val == null) ? "" : val);
                    });
                } else {
                    values.add(field.get(data));
                }
                field.setAccessible(false);
            }));
            stringBuffer.append(values.stream().map(value -> (value == null) ? "" : "\"" + value.toString() + "\"").collect(
                    Collectors.joining(",", "", "\n")));
        }));
        data = stringBuffer.toString();
    }

    private List<Field> getAllFields(List<Field> fields, Class<?> type) {
        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }
        fields.addAll(Arrays.asList(type.getDeclaredFields()));
        return fields;
    }

    static <T> Consumer<T> throwingConsumerWrapper(ThrowingConsumer<T, Exception> throwingConsumer) {
        return t -> {
            try {
                throwingConsumer.accept(t);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new CSVException(ex);
            }
        };
    }
}
