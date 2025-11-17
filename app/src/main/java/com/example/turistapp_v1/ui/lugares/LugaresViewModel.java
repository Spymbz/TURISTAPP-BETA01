package com.example.turistapp_v1.ui.lugares;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LugaresViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public LugaresViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is lugares fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}