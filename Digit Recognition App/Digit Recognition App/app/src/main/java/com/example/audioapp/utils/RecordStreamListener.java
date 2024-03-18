package com.example.digitrecognitionapp.utils;

public interface RecordStreamListener {
    void recordOfByte(byte[] data, int begin, int end);
}
