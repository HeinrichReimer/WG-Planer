package com.heinrichreimersoftware.wg_planer.dialog;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.heinrichreimersoftware.wg_planer.R;
import com.heinrichreimersoftware.wg_planer.adapters.SubjectAdapter;
import com.heinrichreimersoftware.wg_planer.data.TeachersContentHelper;
import com.heinrichreimersoftware.wg_planer.structure.CalendarInterval;
import com.heinrichreimersoftware.wg_planer.structure.Lesson;
import com.heinrichreimersoftware.wg_planer.structure.LessonTimeFactory;
import com.heinrichreimersoftware.wg_planer.structure.Teacher;
import com.heinrichreimersoftware.wg_planer.structure.TeacherSubject;
import com.heinrichreimersoftware.wg_planer.utils.ColorUtils;
import com.heinrichreimersoftware.wg_planer.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator;

public class LessonDialogBuilder extends MaterialDialog.Builder {

    @Bind(R.id.layoutSubject)
    LinearLayout layoutSubject;
    @Bind(R.id.subject)
    TextView subject;
    @Bind(R.id.teacher)
    TextView teacher;
    @Bind(R.id.room)
    TextView room;
    @Bind(R.id.day)
    TextView day;
    @Bind(R.id.hours)
    TextView hours;
    @Bind(R.id.time)
    TextView time;
    @Bind(R.id.layoutList)
    LinearLayout layoutList;
    @Bind(R.id.list)
    RecyclerView list;

    public LessonDialogBuilder(Context context, Lesson lesson) {
        super(context);

        positiveText(R.string.action_close);

        View root = LayoutInflater.from(context).inflate(R.layout.dialog_view_lesson, null);
        ButterKnife.bind(this, root);

        List<TeacherSubject> subjects = lesson.getSubjects();

        String subjectText = "";
        int color;

        if (subjects.size() > 1) {
            List<Integer> colors = new ArrayList<>();

            for (int i = 0; i < subjects.size(); i++) {
                if (i != 0) {
                    subjectText += ", ";
                }
                subjectText += subjects.get(i).getShorthand();

                colors.add(subjects.get(i).getColor());
            }

            color = ColorUtils.averageColor(colors);
        } else {
            color = subjects.get(0).getColor();
            subjectText = subjects.get(0).getFullName();
        }

        Drawable colorIndicator = context.getResources().getDrawable(R.drawable.circle);
        if (colorIndicator != null) {
            colorIndicator.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            icon(colorIndicator);
        }

        title(subjectText);

        day.setText(Utils.dayToString(lesson.getDay()));

        if (lesson.getFirstLessonNumber() == lesson.getLastLessonNumber()) {
            hours.setText("" + lesson.getFirstLessonNumber());
        } else {
            hours.setText(lesson.getFirstLessonNumber() + " - " + lesson.getLastLessonNumber());
        }

        CalendarInterval lessonTime = LessonTimeFactory.fromLesson(lesson);
        time.setText(lessonTime.toString());


        if (subjects.size() > 1 || subjects.size() == 0) {
            layoutSubject.setVisibility(View.GONE);
        } else {
            subject.setText(subjects.get(0).getFullName());

            Teacher teacher = TeachersContentHelper.getTeacher(context, subjects.get(0).getTeacher());
            if (teacher != null) {
                this.teacher.setText(teacher.getLastName());
            } else {
                this.teacher.setText(subjects.get(0).getTeacher());
            }

            room.setText(subjects.get(0).getRoom());

            layoutList.setVisibility(View.GONE);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        list.setLayoutManager(layoutManager);

        RecyclerView.ItemAnimator animator = new FadeInUpAnimator();
        list.setItemAnimator(animator);

        SubjectAdapter adapter = new SubjectAdapter();
        adapter.addAll(lesson.getSubjects());
        list.setAdapter(adapter);

        list.getLayoutParams().height = adapter.getItemCount() * getContext().getResources().getDimensionPixelSize(R.dimen.list_item_height_icon);

        customView(root, true);
    }
}