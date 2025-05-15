package com.example.planitout.ViewContainer;

import android.view.View;
import android.widget.TextView;

import com.example.planitout.R;
import com.kizitonwose.calendar.view.ViewContainer;

public class DayViewContainer extends ViewContainer {
    public TextView textView;
    public View dotCreated, dotAttendee, dotBoth;

    public DayViewContainer(View view) {
        super(view);
        textView = view.findViewById(R.id.calendarDayText);
        dotCreated = view.findViewById(R.id.dotCreated);
        dotAttendee = view.findViewById(R.id.dotAttendee);
        dotBoth = view.findViewById(R.id.dotBoth);
    }
}

