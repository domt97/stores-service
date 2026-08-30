package com.dotran.example.store.application.impex.template;

import lombok.extern.slf4j.Slf4j;

/***
 *  Abstract template for import operations.
 * @param <C> Context
 * @param <D> Data
 * @param <R> Request
 */
@Slf4j
public abstract class AbstractImportTemplate <C, D, R> {

    public ImportResult execute(C context, D data) {
        // 1. Validate
        validate(context, data);

        // 2. Transform data
        R request = transform(context, data);

        // 3. Process - main process
        ImportResult importResult = this.process(context, request);

        // 4. Post import
        postImport(context, request, importResult);

        return importResult;
    }

    protected abstract void validate(C context, D data);

    protected abstract R transform(
            C context,
            D data
    );

    protected abstract ImportResult process(C context, R request);

    protected void postImport(C context, R request, ImportResult importResult) {
        // Default implementation does nothing
    }
}
