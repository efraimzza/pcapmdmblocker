package com.emanuelef.remote_capture.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import com.emanuelef.remote_capture.R;
import com.emanuelef.remote_capture.Utils;

public class AboutActivitya extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setTheme(this);
        setContentView(R.layout.activity_about);
        try{
            if(getActionBar().isShowing())
                getActionBar().hide();
        }catch(Exception e){}
        TextView tvAboutContent = findViewById(R.id.tv_about_content);
        tvAboutContent.setText("אפליקציית ניהול מכשירים (MDM)\n\n" +
                               "יישום זה נועד לסייע בניהול הגדרות המכשיר והאפליקציות וניהול תעבורת אינטרנט במכשירים מנוהלים. " +
                               "דורש הרשאת מנהל מכשיר.\n"+
                               "אין באפליקציה שום לקיחת אחריות על פריצות או עקיפות שיכולות להיות במכשיר שלכם וכמובן שאין לקיחת אחריות על המכשיר.\n"+
                               "האפליקציות הפתוחות במסלול הכי פתוח (השם של המסלול הוא - \"מולטימדיה\")"+
                               "מייל, דרייב, צ'אט, מפות בלי תמונות, זינג, מיוזיק ווליום, וייז, פנגו, מוביט, ביט, קול הלשון, ועוד\n\n"+
                               "עלות החסימה - חינמית לגמרי.\n\n"+
                               "מטרת החסימה - לאפשר לכל יהודי באשר הוא לחסום את מכשיר האנדרואיד שלו מכל סוג שהוא ברמה שהוא צריך.\n\n"+
                               "סיבת פיתוח החסימה - כי לא ראיתי (בזמן שהתחלתי לפתח את זה) חסימה טובה מותאמת אישית וחינמית.\n\n"+
                               "שם וסוג החסימה ברמה הטכנית - חסימת mdm = אפשרויות ניהול מכשיר באופן רשמי מאנדרואיד. (לא שכל החסימה מבוסס על נגישות וקומבינות... רק חלק קטן למי שחייב לדוגמא וואטסאפ בלית ברירה)\n\n");
    }
}
