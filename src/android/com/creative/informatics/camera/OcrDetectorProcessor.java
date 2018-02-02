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
package com.google.android.gms.samples.vision.ocrreader;

import android.graphics.RectF;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.samples.vision.ocrreader.ui.camera.GraphicOverlay;
import com.google.android.gms.samples.vision.ocrreader.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;

import java.util.List;

/**
 * A very simple Processor which receives detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private boolean[] block_f;
    OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay) {
        mGraphicOverlay = ocrGraphicOverlay;
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
        SparseArray<TextBlock> items = detections.getDetectedItems();
        String[] OCR_result=find_amount(items);
        int n=0;
        for (int i=0;i<OCR_result.length;i++)
        {
            if(OcrCaptureActivity.resultStr[i]==null || OcrCaptureActivity.resultStr[i]=="" )

                OcrCaptureActivity.resultStr[i]=OCR_result[i];
            if( OcrCaptureActivity.resultStr[i]!=null &&  OcrCaptureActivity.resultStr[i]!="")
                n++;
        }

        for (int i = 0; i < items.size(); ++i) {
            if(!block_f[i])
                continue;
            TextBlock item = items.valueAt(i);
            RectF rect = new RectF(item.getBoundingBox());
            OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item);
            mGraphicOverlay.add(graphic);
        }
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
}
