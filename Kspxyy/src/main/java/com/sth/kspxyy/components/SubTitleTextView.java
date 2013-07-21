package com.sth.kspxyy.components;

import android.content.Context;
import android.widget.TextView;
import com.sth.kspxyy.subtitle.Caption;

public class SubTitleTextView extends TextView {
    private Caption caption;

    public SubTitleTextView(Context context) {
        super(context);
    }

    public Caption getCaption() {
        return caption;
    }

    public void setCaption(Caption caption) {
        this.caption = caption;
        this.setText(caption.content);
    }
}
