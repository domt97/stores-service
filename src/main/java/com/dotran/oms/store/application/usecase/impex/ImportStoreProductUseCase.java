package com.dotran.oms.store.application.usecase.impex;

import com.dotran.oms.core.io.template.imp.ImportResult;
import com.dotran.oms.store.application.command.impex.ImportStoreProductCmd;

public interface ImportStoreProductUseCase {

    ImportResult execute(ImportStoreProductCmd importCmd);
}
