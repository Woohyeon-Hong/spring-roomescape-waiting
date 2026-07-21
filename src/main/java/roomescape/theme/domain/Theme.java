package roomescape.theme.domain;

import java.util.Objects;
import lombok.Getter;

@Getter
public class Theme {

    private final Long id;
    private final String name;
    private final String description;
    private final String thumbnailUrl;
    private final Long amount;

    public Theme(Long id, String name,
                 String description, String thumbnailUrl, Long amount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.amount = amount;
    }

    public static Theme of(String name, String description,
                           String thumbnailUrl, Long amount) {
        return new Theme(null, name, description, thumbnailUrl, amount);
    }

    public Theme updateId(Long id) {
        return new Theme(
                id,
                this.name,
                this.description,
                this.thumbnailUrl,
                this.amount
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Theme theme = (Theme) o;
        return id != null && Objects.equals(getId(), theme.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
