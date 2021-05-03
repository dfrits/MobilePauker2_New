package de.daniel.mobilepauker2.models;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

public class MPEditText extends AppCompatEditText {

    public MPEditText(Context context) {
        super(context);
    }

    public MPEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MPEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /*public void setCard(CardSide cardside) {
        setText(cardside.getText());
        setFont(cardside.getFont());
    }

    public void setFont(@Nullable Font font) {
        ModelManager.instance().setFont(font, this);
    }*/
}
