package com.dotran.oms.store.application.usecase.impex;

import com.dotran.oms.store.application.command.impex.CreateImportUploadCmd;
import com.dotran.oms.store.application.dto.impex.ImportUploadDto;

public interface CreateImportUploadUseCase {

    ImportUploadDto create(CreateImportUploadCmd cmd);
}
