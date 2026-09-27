package com.dotran.oms.store.application.files;

import java.io.InputStream;

public interface ObjectStorageReader {

    InputStream read(String path, String key);
}
