/*
 *    Debrief - the Open Source Maritime Analysis Application
 *    http://debrief.info
 *
 *    (C) 2000-2014, PlanetMayo Ltd
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the Eclipse Public License v1.0
 *    (http://www.eclipse.org/legal/epl-v10.html)
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 */
package com.borlander.rac353542.bislider.impl;

import com.borlander.rac353542.bislider.*;

class Segmenter implements Disposable {
    private final BiSliderDataModel myDataModel;
    private final BiSliderUIModel myUiModel;
    private ColorInterpolation myInterpolation;
    private double myCachedTotalMin;
    private double myCachedTotalMax;
    private double myCachedSegmentSize;
    private ColoredSegment[] mySegments;
    private final BiSliderDataModel.Listener myDataModelListener;
    private final BiSliderUIModel.Listener myUIModelListener;
    
    public Segmenter(BiSliderDataModel dataModel, BiSliderUIModel uiModel){
        myDataModel = dataModel;
        myUiModel = uiModel;
        cacheDataModelValues();
        updateInterpolation();
        computeSegments();
        myDataModelListener = new BiSliderDataModel.Listener(){
            public void dataModelChanged(BiSliderDataModel model, boolean moreChangesExpectedInNearFuture) {
                onExternalChanges();
            }
        };
        myDataModel.addListener(myDataModelListener);
        
        myUIModelListener = new BiSliderUIModel.Listener(){
            public void uiModelChanged(BiSliderUIModel model) {
                onExternalChanges();
            }
        };
        myUiModel.addListener(myUIModelListener);
    }
    
    public void freeResources() {
        myDataModel.removeListener(myDataModelListener);
        myUiModel.removeListener(myUIModelListener);
        disposeSegments();
    }
    
    public ColoredSegmentEnumeration allSegments(){
        return new SegmentsArrayAsEnumeration(mySegments);
    }
    
    public ColoredSegment getSegment(double value){
        value = Math.max(value, myCachedTotalMin);
        value = Math.min(value, myCachedTotalMax);
        double segmentSize = myDataModel.getSegmentLength();
        int index = (int)Math.floor((value - myCachedTotalMin) / segmentSize);
        index = Math.min(index, mySegments.length - 1);
        return mySegments[index];
    }
    
    public ColoredSegmentEnumeration segments(double minValue, double maxValue){
        minValue = Math.max(minValue, myCachedTotalMin);
        maxValue = Math.min(maxValue, myCachedTotalMax);
        
        double segmentSize = myDataModel.getSegmentLength();
        int startIndex = (int)Math.floor((minValue - myCachedTotalMin) / segmentSize);
        int lastIndex = (int)Math.ceil((maxValue - myCachedTotalMin) / segmentSize);
        return new SegmentsArrayAsEnumeration(mySegments, startIndex, lastIndex);
    }
    
    private void computeSegments() {
        double segmentSize = myDataModel.getSegmentLength();
        int segmentsCount = (int)Math.ceil(myDataModel.getTotalDelta() / segmentSize);
        mySegments = new ColoredSegment[segmentsCount];
        double nextSegmentStart = myDataModel.getTotalMinimum();
        for (int i = 0; i < segmentsCount; i++, nextSegmentStart += segmentSize){
            ColorDescriptor descriptor = new ColorDescriptor(myInterpolation.interpolateRGB(nextSegmentStart));
            mySegments[i] = new ColoredSegment(nextSegmentStart, nextSegmentStart + segmentSize, descriptor);
        }
    }

    private void cacheDataModelValues(){
        myCachedTotalMax = myDataModel.getTotalMaximum();
        myCachedTotalMin = myDataModel.getTotalMinimum();
        myCachedSegmentSize = myDataModel.getSegmentLength();
    }
    
    private boolean isSignificantChanges(){
        return myCachedSegmentSize != myDataModel.getSegmentLength() ||
                myCachedTotalMax != myDataModel.getTotalMaximum() || 
                myCachedTotalMin != myDataModel.getTotalMinimum() || 
                !myInterpolation.isSameInterpolationMode(myUiModel.getColorInterpolation());
    }
    
    private void disposeSegments(){
        if (mySegments != null){
            for (int i = 0; i < mySegments.length; i++){
                mySegments[i].getColorDescriptor().freeResources();
            }
            mySegments = null;
        }
    }
    
    private void updateInterpolation(){
        ColorInterpolation interpolation = myUiModel.getColorInterpolation();
        if (myInterpolation != null && myInterpolation.isSameInterpolationMode(interpolation)){
            return;
        }
        myInterpolation = interpolation;
        myInterpolation.setContext(myUiModel.getMinimumRGB(), myUiModel.getMaximumRGB(), myDataModel.getTotalMinimum(), myDataModel.getTotalMaximum());
    }
    
    void onExternalChanges() {
        if (isSignificantChanges()){
            updateInterpolation();
            disposeSegments();
            computeSegments();
        }
        cacheDataModelValues();        
    }
    
    private static class SegmentsArrayAsEnumeration implements ColoredSegmentEnumeration {
        private int myNextIndex;
        private final ColoredSegment[] myArray;
        private final int myLastIndexExclusive;
        
        public SegmentsArrayAsEnumeration(ColoredSegment[] segments){
            this(segments, 0, segments.length);
        }
        
        public SegmentsArrayAsEnumeration(ColoredSegment[] segments, int startIndexInclusive, int lastIndexExclusive){
            myNextIndex = startIndexInclusive;
            myArray = segments;
            myLastIndexExclusive = lastIndexExclusive;
        }
        
        public boolean hasMoreElements() {
            return myNextIndex < myLastIndexExclusive;
        }
        
        public ColoredSegment nextElement() {
            return next();
        }
        
        public ColoredSegment next() {
            return myArray[myNextIndex++];
        }
    }
    
}
