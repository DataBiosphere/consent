package org.broadinstitute.consent.http.util.gson;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Writes an enum that declares a Jackson {@code @JsonValue} as that value, the form the API
 * documents and accepts, instead of its constant name. Reading accepts either.
 */
public class JsonValueEnumTypeAdapterFactory implements TypeAdapterFactory {

  @Override
  @SuppressWarnings("unchecked")
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    Class<? super T> rawType = type.getRawType();
    if (!Enum.class.isAssignableFrom(rawType) || rawType == Enum.class) {
      return null;
    }
    // A constant with its own body is an anonymous subclass of the enum.
    if (!rawType.isEnum()) {
      rawType = rawType.getSuperclass();
    }
    Optional<Method> valueMethod =
        Arrays.stream(rawType.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(JsonValue.class))
            .filter(method -> method.getParameterCount() == 0)
            .findFirst();
    if (valueMethod.isEmpty()) {
      return null;
    }
    Map<T, String> valueByConstant = new HashMap<>();
    Map<String, T> constantByText = new HashMap<>();
    for (Object constant : rawType.getEnumConstants()) {
      String value = invoke(valueMethod.get(), constant);
      valueByConstant.put((T) constant, value);
      // Names win over values, as in Gson's own enum adapter.
      constantByText.put(((Enum<?>) constant).name(), (T) constant);
      if (value != null) {
        constantByText.putIfAbsent(value, (T) constant);
      }
    }
    return new TypeAdapter<T>() {
      @Override
      public void write(JsonWriter out, T value) throws IOException {
        out.value(valueByConstant.get(value));
      }

      @Override
      public T read(JsonReader in) throws IOException {
        return constantByText.get(in.nextString());
      }
    }.nullSafe();
  }

  private static String invoke(Method valueMethod, Object constant) {
    try {
      Object value = valueMethod.invoke(constant);
      return value == null ? null : value.toString();
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(
          "Cannot read the @JsonValue of " + constant.getClass().getName(), e);
    }
  }
}
