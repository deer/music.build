package build.music.transform;

public interface Transform<T> {
    T apply(T input);

    default Transform<T> andThen(final Transform<T> next) {
        return input -> next.apply(this.apply(input));
    }

    default Transform<T> repeat(final int times) {
        Transform<T> result = identity();
        for (int i = 0; i < times; i++) {
            result = result.andThen(this);
        }
        return result;
    }

    static <T> Transform<T> identity() {
        return input -> input;
    }
}
