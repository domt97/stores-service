package com.dotran.example.store.common.domain;

/*
 This is just a marker class to distinguish entities from the aggregate entity
 */
public abstract class AggregateRoot<ID> extends BaseDomain<ID> {
}
