/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.creative.informatics.camera;

import com.creative.informatics.ui.GraphicOverlay;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A very simple Processor which receives detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {
    private static final String TAG = OcrDetectorProcessor.class.getSimpleName();

    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private boolean[] block_f;
    private DetectionDictInfo[] mDictInfoList;
    private static JSONObject POSTAL_CODES;

    OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay) {
        mGraphicOverlay = ocrGraphicOverlay;

        mDictInfoList = new DetectionDictInfo[OcrCaptureActivity.ocrDict.size()];
        for( int i=0; i<mDictInfoList.length; i++){
            mDictInfoList[i] = new DetectionDictInfo();
            mDictInfoList[i].dict = OcrCaptureActivity.ocrDict.get(i);
        }

        initPostalCode();
    }

    /**
     * Called by the detector to deliver detection results.
     * If your application called for it, this could be a place to check for
     * equivalent detections by tracking TextBlocks that are similar in location and content from
     * previous frames, or reduce noise by eliminating TextBlocks that have not persisted through
     * multiple detections.
     */
    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        mGraphicOverlay.clear();
        final SparseArray<TextBlock> items = detections.getDetectedItems();
        for( int i=0; i<mDictInfoList.length; i++){

            //mDictInfoList[i].mIndexOfKey = -1;
            mDictInfoList[i].bSelected = false;
            mDictInfoList[i].mValueText = null;
            mDictInfoList[i].mKeywordBlock = null;
            mDictInfoList[i].mIndexInKeyBlock = -1;
//            mDictInfoList[i].mValueBlock = null;
//            mDictInfoList[i].mIndexInValueBlock = -1;
        }

        block_f = new boolean[items.size()];
//        Log.e(TAG, "receiveDetections: 1 >>"+items.size());

        find_keyword(items);
        find_value(items);
        Set<OcrGraphic> graphics = new HashSet<OcrGraphic>();

        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item, Color.YELLOW);

            graphics.add(graphic);
        }

        for( DetectionDictInfo info : mDictInfoList){
            if( info.mKeywordBlock != null){
                OcrGraphic graphic;

                int color;
                if( info.bSelected )
                    color = Color.RED;
                else
                    color = Color.GREEN;

                if( info.mValueText != null ) {
                    graphic = new OcrGraphic(mGraphicOverlay, info.mValueText, color);
                    graphics.add(graphic);
                }

                if( info.mIndexInKeyBlock >= 0) {
                    Text keywordText = info.mKeywordBlock.getComponents().get(info.mIndexInKeyBlock);
                    graphic = new OcrGraphic(mGraphicOverlay, keywordText, color);
                    graphics.add(graphic);
                }
            }
        }
        mGraphicOverlay.addAll(graphics);

/*        String[] OCR_result=find_amount(items);
        int n=0;

        synchronized (OcrCaptureActivity.obj) {
            for (int i = 0; i < OCR_result.length; i++) {
                if (OcrCaptureActivity.resultStr[i] == null || OcrCaptureActivity.resultStr[i] == "")

                    OcrCaptureActivity.resultStr[i] = OCR_result[i];
                if (OcrCaptureActivity.resultStr[i] != null && OcrCaptureActivity.resultStr[i] != "")
                    n++;
            }
        }

        for (int i = 0; i < items.size(); ++i) {
            // if(!block_f[i])
            //     continue;
            TextBlock item = items.valueAt(i);
            OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item);
            mGraphicOverlay.add(graphic);
        }*/
    }
    private boolean checkServiceAddressEx(TextBlock block){
        for( DetectionDictInfo info : mDictInfoList) {
            if ( !info.dict.name.toLowerCase().contains("service address")) continue;
            if( !info.dict.resKeyword.isEmpty() ) continue;

            if (info.mIndexOfKey >= 0) break;
            if (info.mKeywordBlock != null) break;

            JSONArray postal = POSTAL_CODES.optJSONArray(OcrCaptureActivity.ocrCountry);
            if (postal != null) {
                for( int i=0; i<postal.length(); i++){
                    List<? extends Text> list = block.getComponents();
                    for( int j=0; j<list.size(); j++){
                        Text item = list.get(j);
                        Pattern p = Pattern.compile(postal.optString(i));
                        if( p.matcher(item.getValue()).find() ) {
                            info.mKeywordBlock = block;
                            info.mValueText = item;
                            return true;
                        }
                    }
                }
            } else
                return false;
        }
        return false;
    }
    private boolean find_keyword(SparseArray<TextBlock> blocks){

        for(int i=0; i<blocks.size(); i++){
            TextBlock item = blocks.valueAt(i);
            List<? extends Text> list = item.getComponents();
            for( int j=0; j<list.size(); j++){
                Text component = list.get(j);
                for (DetectionDictInfo info : mDictInfoList) {
                    int inxKey = info.dict.getIndexKeywords(component.getValue());
                    if (inxKey > -1) {
                        info.mIndexOfKey = inxKey;
                        info.mKeywordBlock = item;
                        info.mIndexInKeyBlock = j;
                        block_f[i] = true;
                        break;
                    }
                }
            }

            if( checkServiceAddressEx(item) )
                block_f[i] = true;
        }

        return true;
    }

    private void find_value(SparseArray<TextBlock> blocks){

        for (DetectionDictInfo info : mDictInfoList) {
            if( info.mKeywordBlock!=null ){
                if( find_value_in_text(info) ) continue;

                if( find_value_in_right(blocks, info) ) continue;

                if( find_value_in_below(info) ) continue;
            }
        }

    }
    private boolean find_value_in_text(DetectionDictInfo info){
        if( info.mIndexInKeyBlock < 0 ) {
            // Service Address without keyword
            if( !info.dict.name.equalsIgnoreCase("service address")) return false;
            if( !info.dict.resKeyword.isEmpty() ) return false;

            ArrayList<String> builder = new ArrayList<String>();
            for( Text text : info.mKeywordBlock.getComponents()){

                if( text.getValue().matches("^[0-9,.\\s]+$") ) {
                    builder.clear();
                }else if( text.getValue().matches("(?i:^[a-z0-9,.\\s]+$)") ) {
                    builder.add(text.getValue());
                } else {
                    builder.clear();
                }
                if( info.mValueText.getBoundingBox().top == text.getBoundingBox().top ) break;
            }
            String value = TextUtils.join(", ", builder);

            if( info.dict.checkMatchValuePattern(value) != null) {
                info.mValueText = info.mKeywordBlock.getComponents().get(0);

                if (info.dict.setValueIfAcceptable(value)) {
                    info.bSelected = true;
                    Log.d(TAG, "find_value_in_text: a new Value:" + info.dict.getDisplayString());
                }
                return true;
            }
        } else {
            Text keyword = info.mKeywordBlock.getComponents().get(info.mIndexInKeyBlock);
            for(String key : info.dict.keywords){
                int offset = keyword.getValue().toLowerCase().indexOf(key.toLowerCase());
                if( offset < 0) continue;
                String value = keyword.getValue().substring(offset + key.length()).trim();

                if( info.dict.checkMatchValuePattern(value) != null) {
                    info.mValueText = keyword;

                    if (info.dict.setValueIfAcceptable(value)) {
                        info.bSelected = true;
                        info.dict.resKeyword = info.dict.keywords.get(info.mIndexOfKey);
                        Log.d(TAG, "find_value_in_text: A new Value:" + info.dict.getDisplayString());
                    }
                    return true;
                }
            }
        }

        return false;
    }

    private boolean find_value_in_right(SparseArray<TextBlock> blocks, DetectionDictInfo info){
        if( info.mIndexInKeyBlock < 0 ) return false;

        Text keyword = info.mKeywordBlock.getComponents().get(info.mIndexInKeyBlock);
        Rect rcKeyword = new Rect(keyword.getBoundingBox());
        ArrayList<Text> result = new ArrayList<Text>();
        for (int i=0;i<blocks.size();i++) {
            TextBlock block = blocks.valueAt(i);
            for( Text text : block.getComponents()){
                Rect rcText = new Rect(text.getBoundingBox());
                if( Math.abs(rcText.top-rcKeyword.top) > 10 ) continue;
                if( rcKeyword.right > rcText.left) continue;

                result.add(text);
            }
        }

        if( result.isEmpty() ) return false;

        Collections.sort(result, new Comparator<Text>() {
            @Override
            public int compare(Text o1, Text o2) {
                return o1.getBoundingBox().left - o2.getBoundingBox().left;
            }
        });

        Text text = result.get(0);
        if( info.dict.checkMatchValuePattern(text.getValue()) != null) {
            info.mValueText = text;
            if( info.mIndexOfKey < 0) info.dict.resValue="";

            if (info.dict.setValueIfAcceptable(text.getValue())) {
                info.bSelected = true;
                info.dict.resKeyword = info.dict.keywords.get(info.mIndexOfKey);
                Log.d(TAG, "find_value_in_right: " + info.dict.getDisplayString());
            }
            return true;
        }
        return false;
    }

    private boolean find_value_in_below(DetectionDictInfo info){
        if( info.mIndexInKeyBlock < 0 ) return false;
        if( !info.dict.hasPatterns() ) return false;

        Text keyword = info.mKeywordBlock.getComponents().get(info.mIndexInKeyBlock);
        Rect rcKeyword = new Rect(keyword.getBoundingBox());
        ArrayList<Text> result = new ArrayList<Text>();
        List<? extends Text> components = info.mKeywordBlock.getComponents();

        for( Text text : components){
            Rect rcText = text.getBoundingBox();
            if( rcKeyword.top >= rcText.top ) continue;

            result.add(text);
        }

        if( result.isEmpty() ) return false;

        Collections.sort(result, new Comparator<Text>() {
            @Override
            public int compare(Text o1, Text o2) {
                return o1.getBoundingBox().top - o2.getBoundingBox().top;
            }
        });

        Text text = result.get(0);
        if( info.dict.checkMatchValuePattern(text.getValue()) != null) {
            info.mValueText = text;
            if( info.mIndexOfKey < 0) info.dict.resValue="";

            if (info.dict.setValueIfAcceptable(text.getValue())) {
                info.bSelected = true;
                info.dict.resKeyword = info.dict.keywords.get(info.mIndexOfKey);
                Log.d(TAG, "find_value_in_below: " + info.dict.getDisplayString());
            }
            return true;
        }
        return false;
    }

    public String[] find_amount(SparseArray<TextBlock> items)
    {
        String[] result=new String[8];
        for (int i=0;i<result.length;i++)
            result[i]="";
        boolean[] item_f=new boolean[items.size()];
        block_f=new boolean[items.size()];
        for (int i=0;i<items.size();i++)
        {
            TextBlock item = items.valueAt(i);
            List<? extends Text> textComponents = item.getComponents();
            for(Text currentText : textComponents) {
                String block_text=currentText.getValue();
                if(check_text(block_text)) {
                    item_f[i]=true;
                    block_f[i]=true;
                }
            }
        }
        for (int i=0;i<items.size();i++)
        {
            if(!item_f[i])
                continue;
            TextBlock item = items.valueAt(i);
            RectF rect = new RectF(item.getBoundingBox());
            List<? extends Text> textComponents = item.getComponents();
            int count=-1;
            for(Text currentText : textComponents) {
                String block_text=currentText.getValue();
                count++;
                int index=match_index(block_text);
                switch (index)
                {
                    case 0:
                        result[0]=block_text;
                        break;
                    case 1:
                    if(check_month(block_text))
                        result[1]=block_text;
                    else
                    {
                        String str1=month_string(textComponents,count);
                        if(str1!="")
                            result[1]=str1;
                        else
                        {
                            result[1]=find_block(rect,0,count,items);
                        }
                    }
                    break;
                    case 2:
                        if(check_month(block_text))
                            result[2]=block_text;
                        else
                        {
                            String str1=month_string(textComponents,count);
                            if(str1!="")
                                result[2]=str1;
                            else
                            {
                                result[2]=find_block(rect,0,count,items);
                            }
                        }
                        break;
                    case 3:
                        if(check_month(block_text))
                            result[3]=block_text;
                        else
                        {
                            String str1=month_string(textComponents,count);
                            if(str1!="")
                                result[3]=str1;
                            else
                            {
                                result[3]=find_block(rect,0,count,items);
                            }
                        }
                        break;
                    case 4:
                        String str1=amount_string(textComponents,count);
                        if(str1!="")
                            result[4]=str1;
                        else
                        {
                            result[4]=find_block(rect,1,count,items);
                        }

                        break;
                    case 5:
                        String str5=amount_string(textComponents,count);
                        if(str5!="")
                            result[5]=str5;
                        else
                        {
                            result[5]=find_block(rect,1,count,items);
                        }

                        break;
                    case 6:
                        String str6=amount_string(textComponents,count);
                        if(str6!="")
                            result[6]=str6;
                        else
                        {
                            result[6]=find_block(rect,1,count,items);
                        }

                        break;
                    case 7:
                        String str7=amount_string(textComponents,count);
                        if(str7!="")
                            result[7]=str7;
                        else
                        {
                            result[7]=find_block(rect,1,count,items);
                        }

                        break;
                }
            }
        }
        return result;
    }
    private String find_block(RectF mrect, int kind,int count,SparseArray<TextBlock> items)
    {
        float M=100000;
        int mindex=-1;
        for (int i=0;i<items.size();i++) {
            TextBlock item = items.valueAt(i);
            RectF rect = new RectF(item.getBoundingBox());
            if(rect.left<mrect.right)
                continue;
            if(rect.left==mrect.left && rect.top==mrect.top)
                continue;
            if(Math.abs(rect.top-mrect.top)>10)
                continue;
            if(Math.abs(rect.bottom-mrect.bottom)>10)
                continue;
            if(M>rect.left)
            {
                M=rect.left;
                mindex=i;
            }
        }
        TextBlock mitem = items.valueAt(mindex);
        block_f[mindex]=true;
        List<? extends Text> textComponents = mitem.getComponents();
        int no=-1;
        if(textComponents.size()>count)
        {
            for(Text currentText : textComponents) {
                no++;
                if(no==count)
                {
                    String block_text = currentText.getValue();
                    if(kind==0)
                    {
                        if(check_month(block_text))
                            return block_text;
                    }
                    else if(kind==1)
                    {
                        if(check_amount(block_text))
                            return block_text;
                    }
                    break;
                }


            }
        }

        return "";
    }
    private String month_string(List<? extends Text> textComponents,int count)
    {
        int no=-1;
        for(Text currentText : textComponents) {
            no++;
            if(no<=count)
                continue;
            String block_text=currentText.getValue();
            if(check_month(block_text))
                return block_text;
        }
        return "";
    }
    private String amount_string(List<? extends Text> textComponents,int count)
    {
        int no=-1;
        for(Text currentText : textComponents) {
            no++;
            if(no<=count)
                continue;
            String block_text=currentText.getValue();
            if(check_amount(block_text))
                return block_text;
        }
        return "";
    }
    private boolean check_amount1(String str)
    {
        String[] str1=str.split(" ");
        for(int i=0;i<str1.length;i++)
        {
            if(str1[i].substring(0,1).contains("$"))
                return true;
        }
        return false;
    }
    private boolean check_amount(String str)
    {
        if(str.trim().substring(0,1).contains("$"))
            return true;
        return false;
    }
    private boolean check_month(String str)
    {
        String[] month_str=new String[]{"jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"};
        for(int i=0;i<month_str.length;i++)
        {
            if(str.trim().toLowerCase().contains(month_str[i]))
                return true;
        }
        return false;
    }
    public int match_index(String str)
    {
        String[] pattern_str=new String[]{"due date","simply pay by","bill issued","total due","total amount due","total amount due with discount","only pay"};
        for(int i=0;i<pattern_str.length;i++)
        {
            if(str.toLowerCase().contains(pattern_str[i]))
                return i+1;
        }
        if(str.trim().contains("Pty") && str.trim().contains("ABN"))
            return 0;
        return -1;
    }
    public boolean check_text(String str)
    {
        String[] pattern_str=new String[]{"due date","simply pay by","bill issued","total due","total amount due","total amount due with discount","only pay"};
        for(int i=0;i<pattern_str.length;i++)
        {
            if(str.trim().toLowerCase().contains(pattern_str[i]))
                return true;
        }
        if(str.trim().contains("Pty") && str.trim().contains("ABN"))
            return true;
        return false;
    }
    /**
     * Frees the resources associated with this detection processor.
     */
    @Override
    public void release() {
        mGraphicOverlay.clear();
    }

    public class DetectionDictInfo {
        public OcrCaptureActivity.OCRDictionary dict;
        public boolean bSelected;

        public int mHeightRate;

        public int mIndexOfKey;

        public TextBlock mKeywordBlock;
        public int mIndexInKeyBlock;

        public Text mValueText;

//        public TextBlock mValueBlock;
//        public int mIndexInValueBlock;

        public DetectionDictInfo(){
            dict = null;
            bSelected = false;
            mHeightRate = 0;
            mIndexOfKey = -1;
            mKeywordBlock = null;
//            mValueBlock = null;
            mValueText = null;
            mIndexInKeyBlock = -1;
//            mIndexInValueBlock = -1;
        }
    }

    private static void initPostalCode(){

        POSTAL_CODES = new JSONObject();
        JSONArray australia = new JSONArray();
        australia.put("VIC[\\s]*[0-9]{4}$");
        australia.put("NSW[\\s]*[0-9]{4}$");
        australia.put("QLD[\\s]*[0-9]{4}$");
        australia.put("NT[\\s]*[0-9]{4}$");
        australia.put("WA[\\s]*[0-9]{4}$");
        australia.put("SA[\\s]*[0-9]{4}$");
        australia.put("TAS[\\s]*[0-9]{4}$");

        try {
            POSTAL_CODES.putOpt("Australia", australia);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
