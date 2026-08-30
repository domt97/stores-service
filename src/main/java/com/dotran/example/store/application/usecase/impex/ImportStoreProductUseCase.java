package com.dotran.example.store.application.usecase.impex;

import com.dotran.example.store.application.command.impex.ImportStoreProductCmd;
import com.dotran.example.store.application.impex.template.ImportResult;

public interface ImportStoreProductUseCase {

    ImportResult execute(ImportStoreProductCmd importCmd);
}
