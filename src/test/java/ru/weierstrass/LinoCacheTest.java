package ru.weierstrass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class LinoCacheTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNotIdentifiableValue() {
        LinoCache cache = new LinoCache(10);
        cache.put("any key", "not identifiable value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCollectionContainsNotIdentifiableValue() {
        List<Object> list = new ArrayList<>();
        list.add(new LinoIdentifiable() {
            @Override
            public String getKey() {
                return "test";
            }
        });
        list.add("123123");
        LinoCache cache = new LinoCache(10);
        cache.put("123", list);
    }

    @Test
    public void testSingleKeyStore() {
        LinoIdentifiable value = () -> "123";
        LinoCache cache = new LinoCache(10, 0);
        cache.put("test", value);
        cache.remove("123");
        assertNull(cache.get("test"));
    }

    @Test
    public void testCollectionKeyStore() {
        List<Object> list = new ArrayList<>();
        list.add(new LinoIdentifiable() {
            @Override
            public String getKey() {
                return "test";
            }
        });
        list.add(new LinoIdentifiable() {
            @Override
            public String getKey() {
                return "test1";
            }
        });
        LinoCache cache = new LinoCache(10);
        cache.put("123", list);
        cache.remove("test");
        assertNull(cache.get("123"));
    }

    @Test
    public void testZeroTtlValueStore() throws InterruptedException {
        LinoIdentifiable value = () -> "123";
        LinoCache cache = new LinoCache(10, 0);
        cache.put("test", value);
        Thread.sleep(5000);
        assertEquals(value, cache.get("test"));
    }

    @Test
    public void testFiveSecondTtlValueStore() throws InterruptedException {
        LinoIdentifiable value = () -> "123";
        LinoCache cache = new LinoCache(10, 5000);
        cache.put("test", value);
        Thread.sleep(2000);
        assertEquals(value, cache.get("test"));
        Thread.sleep(3500);
        assertNull(cache.get("test"));
    }

}
