package de.daniel.mobilepauker2.models.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class MPTextView : AppCompatEditText {
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    ) {
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
    }

    constructor(context: Context?) : super(context!!) {} /*public void setCard(CardSide cardside) {
        setText(cardside.getText());

        setFont(cardside.getFont());
    }

    public void setFont(@Nullable Font font) {
        ModelManager.instance().setFont(font, this);
    }*/
}