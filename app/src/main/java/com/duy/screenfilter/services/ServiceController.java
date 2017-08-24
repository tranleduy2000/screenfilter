package com.duy.screenfilter.services;

import android.content.Intent;
import android.support.annotation.Nullable;

interface ServiceController {
    void start(@Nullable Intent intent);

    void pause(@Nullable Intent intent);

    void stop(@Nullable Intent intent);

    void update(@Nullable Intent intent);
}