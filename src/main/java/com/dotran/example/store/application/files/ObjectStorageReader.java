package com.dotran.example.store.application.files;

import java.io.InputStream;

public interface ObjectStorageReader {

    InputStream read(String path, String key);
}
