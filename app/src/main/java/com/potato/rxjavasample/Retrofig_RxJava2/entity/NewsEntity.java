package com.potato.rxjavasample.Retrofig_RxJava2.entity;

import java.util.List;

public class NewsEntity {
    private boolean error;
    private List<NewsResultEntity> results;

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public List<NewsResultEntity> getResults() {
        return results;
    }

    public void setResults(List<NewsResultEntity> results) {
        this.results = results;
    }
}
