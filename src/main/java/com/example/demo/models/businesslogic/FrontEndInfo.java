package com.example.demo.models.businesslogic;

import lombok.Data;

@Data
public class FrontEndInfo {
    
    private boolean isSupportDragAndDrop;
    
    private boolean isSupportMatching;
    
    private boolean isSupportMultiChoice;

    private boolean isSupportSingleChoice;
}
