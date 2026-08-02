package com.dotran.example.store.common.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
public abstract class BaseDomain<ID> {

    protected ID id;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BaseDomain<?> that = (BaseDomain<?>) obj;
        return id.equals(that.id);
    }


    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
