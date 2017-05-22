package ru.weierstrass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KeyGenerator {

    public List<String> generate(Object value) throws IllegalArgumentException {
        List<String> key = new ArrayList<>();
        if (value instanceof Collection) {
            ((Collection) value).forEach(o -> key.add(generateSingle(o)));
        } else {
            key.add(generateSingle(value));
        }
        return key;
    }

    private String generateSingle(Object value) throws IllegalArgumentException {
        if (value instanceof Identifiable) {
            return ((Identifiable) value).getKey();
        }
        throw new IllegalArgumentException(
            "Value should implements " + Identifiable.class.getName() + " interface.");
    }

}
