package de.daniel.mobilepauker2.models;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

public class MPTextView extends AppCompatEditText {

    public MPTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MPTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MPTextView(Context context) {
        super(context);
    }

    /*public void setCard(CardSide cardside) {
        setText(cardside.getText());

        setFont(cardside.getFont());
    }

    public void setFont(@Nullable Font font) {
        ModelManager.instance().setFont(font, this);
    }*/
}
