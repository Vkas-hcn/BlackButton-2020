package com.demo.blackbutton.utils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarUtils {
    final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    final static int MAX_YEAR_NUM = 1;//包含的年份
    final static int MAX_FUTURE_MONTH = 1;//未来月数(1-12)

    //获取当前时间
    public static String getCurrentToday() {
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getCurrentDateString() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR) + "年" + (cal.get(Calendar.MONTH) + 1) + "月" + (cal.get(Calendar.DATE) + "日");
    }
    //个位数补0操作
    public static String getValue(int num) {
        return String.valueOf(num > 9 ? num : ("0" + num));
    }

    //星期几
    private static String getWeekName(int weekNum) {
        String name = "";
        switch (weekNum) {
            case 1:
                name = "周日";
                break;
            case 2:
                name = "周一";
                break;
            case 3:
                name = "周二";
                break;
            case 4:
                name = "周三";
                break;
            case 5:
                name = "周四";
                break;
            case 6:
                name = "周五";
                break;
            case 7:
                name = "周六";
                break;
            default:
                break;
        }
        return name;
    }

    //是否是今天
    public static boolean isToday(String date) {
        boolean b = false;
        Date time = null;
        try {
            time = dateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date today = new Date();
        if (time != null) {
            String nowDate = dateThreadFormat.get().format(today);
            String timeDate = dateThreadFormat.get().format(time);
            if (nowDate.equals(timeDate)) {
                b = true;
            }
        }
        return b;
    }

    private final static ThreadLocal<SimpleDateFormat> dateThreadFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };

    //获取日历中选择的开始和结束日期
    public static ArrayList<String> getResultDates(String date, List<String> dates) {
        ArrayList<String> result = new ArrayList<>();
        String start = dates.get(0);
        String end = dates.get(1);
        if (dateAfterDate(end, date)) {
            result.add(start);
            result.add(date);
        } else if (dateAfterDate(date, start)) {
            result.add(date);
            result.add(end);
        } else {
            int num0 = totalTime(start, date);
            int num1 = totalTime(date, end);
            if (num0 > num1) {
                result.add(date);
                result.add(end);
            } else {
                result.add(start);
                result.add(date);
            }
        }
        return result;
    }

    //判断一个时间在另一个时间之后
    public static boolean dateAfterDate(String startTime, String endTime) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date startDate = format.parse(startTime);
            Date endDate = format.parse(endTime);
            long start = startDate.getTime();
            long end = endDate.getTime();
            if (end > start) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    //计算两个日期的间隔天数
    private static int totalTime(String startTime, String endTime) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = null;
        Date date = null;
        Long l = 0L;
        try {
            date = formatter.parse(startTime);
            long ts = date.getTime();
            date1 = formatter.parse(endTime);
            long ts1 = date1.getTime();
            l = (ts - ts1) / (1000 * 60 * 60 * 24);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return l.intValue();
    }

    /**
     * 获取当前秒
     */
    public static String getCurrentSeconds() {
        Calendar cal = Calendar.getInstance();
        int second = cal.get(Calendar.SECOND);
        StringBuffer buffer = new StringBuffer();
        if (second < 10) {
            buffer.append(0).append(second);
        } else {
            buffer.append(second);
        }
        return buffer.toString();
    }

    /**
     * 获取小时/分钟，单位数前加 0
     */
    public static String getDouble(int min) {
        StringBuffer buffer = new StringBuffer();
        if (min < 10) {
            buffer.append(0).append(min);
        } else {
            buffer.append(min);
        }
        return buffer.toString();
    }

    /**
     * 获取当前时间
     */
    public static String getCurrentTime() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        return hour + ":" + minute + ":" + second;
    }

    /**
     * 判断2个时间大小
     * HH:mm 格式（自己可以修改成想要的时间格式）
     *
     * @param startTime
     * @param endTime
     * @return 1-结束小于开始；2等于；3-结束大于开始
     */
    public static int timeCompare(String startTime, String endTime) {
        int i = 0;
        //注意：传过来的时间格式必须要和这里填入的时间格式相同
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        try {
            //开始时间
            Date date1 = dateFormat.parse(startTime);
            //结束时间
            Date date2 = dateFormat.parse(endTime);
            // 1 结束时间小于开始时间 2 开始时间与结束时间相同 3 结束时间大于开始时间
            if (date2.getTime() < date1.getTime()) {
                //结束时间小于开始时间
                i = 1;
            } else if (date2.getTime() == date1.getTime()) {
                //开始时间与结束时间相同
                i = 2;
            } else if (date2.getTime() > date1.getTime()) {
                //结束时间大于开始时间
                i = 3;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }

    static long CONST_WEEK = 3600 * 1000 * 24 * 7;

    /**
     * 质量压缩
     * 设置bitmap options属性，降低图片的质量，像素不会减少
     * 第一个参数为需要压缩的bitmap图片对象，第二个参数为压缩后图片保存的位置
     * 设置options 属性0-100，来实现压缩（因为png是无损压缩，所以该属性对png是无效的）
     *
     * @param bmp
     * @param file
     */
    public static void qualityCompress(Bitmap bmp, File file) {
        // 0-100 100为不压缩
        int quality = 20;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 把压缩后的数据存放到baos中
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String T_money(int money) {
        int num = 0;
        String[] MoneyChinese = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};//汉字一到九
        String[] ChineseNum = {"", "拾", "百", "千", "万", "亿"};//汉字单位
        System.out.println(ChineseNum[0]);
        Integer Money = new Integer(money);//转化为Integer方便转发类型
        char[] Moneynum = Money.toString().toCharArray();//转换成字符串方便转换成整形
        String[] MoneyChineseNum = new String[Moneynum.length];//用来存放转换后的整形数组
        for (int i = 0; i < Moneynum.length; i++) {
            num = Moneynum[i] - 48;//转换成整形
            MoneyChineseNum[i] = MoneyChinese[num];//用来映射汉字一到九
        }
        StringBuffer MoneyTime = new StringBuffer();//字符缓冲区方便添加
        int nums = 0;//统计要出现的”万“
        int Numss = 0;//统计要出现的”亿“
        for (int i = MoneyChineseNum.length - 1; i >= 0; i--) {

            if (!MoneyChineseNum[i].equals("零")) {
                if (!ChineseNum[nums].equals("万"))
                    MoneyTime.append(ChineseNum[nums]);
            }

            if (nums == 4 && Numss == 0)//添加“万”字因为万字必须出现（必能想千、百、拾，前面有零而省去）
            {
                MoneyTime.append(ChineseNum[nums]);
                nums = 0;
                Numss = 1;
                if (!MoneyChineseNum[i].equals("零")) {
                    MoneyTime.append(MoneyChineseNum[i]);
                }//如果"万"字前有"零"除去万字前的 "零"
            } else if (nums == 4 && Numss == 1)//添加“亿”字因为万字必须出现（必能想千、百、拾，前面有零而省去）
            {
                MoneyTime.append(ChineseNum[nums + 1]);
                nums = 0;
                Numss = 0;
                if (!MoneyChineseNum[i].equals("零")) {
                    MoneyTime.append(MoneyChineseNum[i]);
                }//如果"亿"字前有"零"除去亿字前的 "零"
            } else {
                MoneyTime.append(MoneyChineseNum[i]);
            }
            ++nums;
        }
        return MoneyTime.reverse().toString();
    }


    /*
     * 将时间转换为时间戳
     */
    public static String dateToStamp(String s) throws ParseException {
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = simpleDateFormat.parse(s);
        long ts = date.getTime();
        res = String.valueOf(ts);
        return res;
    }

    /*
     * 将时间戳转换为时间
     */
    public static String stampToDate(String s) {
        String res;
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        long lt = new Long(s);
        Date date = new Date(lt);
        res = simpleDateFormat.format(date);
        return res;
    }

    /**
     * 时间相减得到天数
     *
     * @param beginDateStr
     * @param endDateStr
     * @return
     */
    public static long getDaySub(String beginDateStr, String endDateStr) {
        long day = 0;
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date beginDate;
        Date endDate;
        try {
            beginDate = format.parse(beginDateStr);
            endDate = format.parse(endDateStr);
            day = (endDate.getTime() - beginDate.getTime()) / (24 * 60 * 60 * 1000);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return day;
    }

    /**
     * 两个时间相减得到分钟数
     *
     */
    public static long getMinSub(String beginDateStr, String endDateStr) {
        long min = 0;
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        Date beginDate;
        Date endDate;
        try {
            beginDate = format.parse(beginDateStr);
            endDate = format.parse(endDateStr);
            min = (endDate.getTime() - beginDate.getTime()) / (60 * 1000);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return min;
    }


    public static String getTime(String time) {
        String cTime = "";
//        String otime = time.substring(0, 10);
        String otime = getResultTime(time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = (Date) dateFormat.parseObject(otime);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
            cTime = simpleDateFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return cTime;
    }


    public static String getNextTime(String time) {
        String uTime = getResultTime(time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        Calendar calendar = Calendar.getInstance();
        String next = "";
        try {
            Date uDate = dateFormat.parse(uTime);
            calendar.setTime(uDate);
            calendar.add(Calendar.DATE, 1);
            next = simpleDateFormat.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return next;
    }

    /**
     * @return 当前详细日期
     */
    public static String formatDetailedDateNow(Boolean isEnd) {
        SimpleDateFormat simpleDateFormat;
        if (isEnd) {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        } else {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        }

        Date date = new Date();
        String time = simpleDateFormat.format(date);
        return time;
    }

    /**
     * @return 当前日期
     */
    public static String formatDateNow() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String time = simpleDateFormat.format(date);
        return time;
    }

    /**
     * 转2020-05-10样式
     *
     * @param time
     * @return
     */
    public static String getTimeToDateFormat1(String time) {
        String cTime = "";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        try {
            Date date = (Date) dateFormat.parseObject(time);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            cTime = simpleDateFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return cTime;
    }

    /**
     * 转年月日
     *
     * @param time
     * @return
     */
    public static String getTimeToDateFormat2(String time) {
        String cTime = "";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = (Date) dateFormat.parseObject(time);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
            cTime = simpleDateFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return cTime;
    }

    /**
     * 判断当前时间处于上午还是下午
     *
     * @return apm=0 表示上午，apm=1表示下午
     */
    public static int getAmp() {
        long time = System.currentTimeMillis();
        final Calendar mCalendar = Calendar.getInstance();
        mCalendar.setTimeInMillis(time);

        int hour = mCalendar.get(Calendar.HOUR);
        int apm = mCalendar.get(Calendar.AM_PM);
        return apm;
    }

    public static String getTodayTime() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        return dateFormat.format(date);
    }

    public static String getNextDay() {
        Date date = new Date();
        long time = date.getTime();
        long nextTime = time + 1 * 24 * 60 * 60 * 1000;

        Date next = new Date(nextTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        return dateFormat.format(next);
    }

    /**
     * 计算当月第一天
     *
     * @return
     */
    public static String getFirstofMonth(String dete_format) {
        SimpleDateFormat format = new SimpleDateFormat(dete_format);
        Calendar ca = Calendar.getInstance();
        ca.add(Calendar.MONTH, 0);
        ca.set(Calendar.DAY_OF_MONTH, 1);
        String first = format.format(ca.getTime());
        return first;
    }

    /**
     * 计算当月最后一天
     *
     * @return
     */
    public static String getEndofMonth() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日");
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.DAY_OF_MONTH, ca.getActualMaximum(Calendar.DAY_OF_MONTH));
        String last = format.format(ca.getTime());
        return last;
    }

    /**
     * 计算下个月最后一天
     *
     * @return
     */
    public static String getEndofMonthNext() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日");
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.DAY_OF_MONTH + 1, ca.getActualMaximum(Calendar.DAY_OF_MONTH + 1));
        String last = format.format(ca.getTime());
        return last;
    }

    /**
     * 计算下个月最后一天-毫秒数
     *
     * @return
     */
    public static Long getEndofMonthNext2() {
        Calendar ca = Calendar.getInstance();
        ca.add(Calendar.MONTH, 1);
        ca.set(Calendar.DAY_OF_MONTH, ca.getActualMaximum(Calendar.DAY_OF_MONTH));
        return ca.getTime().getTime();
    }

    /**
     * 计算当月最后一天(yyyy-mm-dd)
     *
     * @return
     */
    public static String getEndofMonthOther() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.DAY_OF_MONTH, ca.getActualMaximum(Calendar.DAY_OF_MONTH));
        String last = format.format(ca.getTime());
        return last;
    }

    /**
     * 计算当前时间T+2天
     *
     * @return
     */
    public static String getNext3Day() {
        Date date = new Date();
        long time = date.getTime();
        long nextTime = time + 3 * 24 * 60 * 60 * 1000;

        Date next = new Date(nextTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        return dateFormat.format(next);
    }

    /**
     * 计算当前时间T+2天
     *
     * @return
     */
    public static String getNext2Day() {
        Date date = new Date();
        long time = date.getTime();
        long nextTime = time + 2 * 24 * 60 * 60 * 1000;

        Date next = new Date(nextTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        return dateFormat.format(next);
    }

    /**
     * 计算当前时间T+n天
     *
     * @return
     */
    public static String getNextNDay(int num) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date dateDay = new Date();
        String time = dateFormat.format(dateDay);
        try {
            Date date = dateFormat.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DATE, num);
            String result = dateFormat.format(calendar.getTime());
            return result;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 计算当前时间T-n天(详细)
     *
     * @return
     */
    public static String getDetailedNextNDay(int num, Boolean isEnd) {
        SimpleDateFormat dateFormat;
        if (isEnd) {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        } else {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        }
        Date dateDay = new Date();
        String time = dateFormat.format(dateDay);
        try {
            Date date = dateFormat.parse(time);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DATE, -num);
            String result = dateFormat.format(calendar.getTime());
            return result;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过配送频率来计算天数
     *
     * @return 天数
     */
    public static int totalDaysViaGap(String startTime, String endTime, int gap) {

        int gapTime = gap + 1;
        int time = 0;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        try {
            Date start = simpleDateFormat.parse(startTime);
            Date end = simpleDateFormat.parse(endTime);
            time = (int) ((end.getTime() - start.getTime()) / 1000 / 60 / 60 / 24);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int dispTime = time / gapTime + 1;
        return dispTime;

    }

    /**
     * 通过星期来计算天数
     *
     * @return 天数
     */
    public static int totalDaysViaWeek(String startTime, String endTime, String weeks) {
        String[] weekList = weeks.split(",");
        Integer[] weekListNum = new Integer[weekList.length];

        for (int i = 0; i < weekList.length; i++) {
            weekListNum[i] = Integer.parseInt(weekList[i]) % 7;
        }
        weekListNum = getReulstArr(weekListNum, weekListNum.length - 1);//将星期按照0，1，2...6的顺序排列

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        Calendar calendarStart = Calendar.getInstance();
        Calendar calendarEnd = Calendar.getInstance();
        try {
            calendarStart.setTime(simpleDateFormat.parse(startTime));
            calendarEnd.setTime(simpleDateFormat.parse(endTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int week = calendarStart.get(Calendar.DAY_OF_WEEK);
        calendarStart.add(Calendar.DATE, -week);
        week = calendarEnd.get(Calendar.DAY_OF_WEEK);
        calendarEnd.add(Calendar.DATE, 7 - week);

        int interval = (int) ((calendarEnd.getTimeInMillis() - calendarStart
                .getTimeInMillis()) / CONST_WEEK);

        Integer[] weekNum = {0, 1, 2, 3, 4, 5, 6};
        Calendar calStart = Calendar.getInstance();
        try {
            calStart.setTime(simpleDateFormat.parse(startTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        int s = calStart.get(Calendar.DAY_OF_WEEK) - 1;
        if (s < 0)
            s = 0;
        int start = weekNum[s];


        Calendar calEnd = Calendar.getInstance();
        try {
            calEnd.setTime(simpleDateFormat.parse(endTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        int n = calEnd.get(Calendar.DAY_OF_WEEK) - 1;
        if (n < 0)
            n = 0;
        int end = weekNum[n];


        int startCount = 0;
        for (int i = 0; i < weekListNum.length; i++) {
            if (start <= weekListNum[i])
                startCount++;
        }

        int endCount = 0;
//
//
        for (int i = 0; i < weekListNum.length; i++) {
            if (end >= weekListNum[i]) {
                endCount++;
            }
        }
        int counts = startCount + endCount;

//        if (interval == 1) {
//            return startCount;
//        } else if (interval == 2) {
//            return counts;
//        } else {
        int weekCount = (interval - 2) * weekListNum.length + counts;
        return weekCount;
//        }

    }

    public static Integer[] getReulstArr(Integer[] a, int time) {
        for (int i = 0; i < time; i++) {
            int temp = a[0];
            for (int j = 1; j < a.length; j++) {
                a[j - 1] = a[j];
            }
            a[a.length - 1] = temp;
        }
        for (int i : a) {
            System.out.println(i);
        }

        return a;
    }


    public static String generateUuidKey() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 字符串的日期格式的计算
     */
    public static String daysBetween(String smdate, String bdate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(sdf.parse(smdate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long time1 = cal.getTimeInMillis();
        try {
            cal.setTime(sdf.parse(bdate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long time2 = cal.getTimeInMillis();
        long between_days = (time2 - time1) / (1000 * 3600 * 24) + 1;

        return String.valueOf(between_days);
    }


    public static int betweenDays(String smdate, String bdate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(sdf.parse(smdate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long time1 = cal.getTimeInMillis();
        try {
            cal.setTime(sdf.parse(bdate));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long time2 = cal.getTimeInMillis();
        long between_days = (time2 - time1) / (1000 * 3600 * 24) + 1;
        return Integer.parseInt(String.valueOf(between_days));
    }

    /**
     * 根据开始时间和间隔天数，计算结束时间
     *
     * @param startTime 开始时间
     * @param days      间隔天数
     * @return stopTime ：结束时间
     */
    public static String upToDay(String startTime, int days) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        long time = 0;
        Calendar calendar = Calendar.getInstance();
        try {
            Date startDate = simpleDateFormat.parse(startTime);

            calendar.setTime(startDate);
            calendar.add(Calendar.DATE, days);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date stopDate = calendar.getTime();
        String stopTime = simpleDateFormat.format(stopDate);
        return stopTime;
    }

    /**
     * 根据开始时间和间隔天数，计算结束时间
     *
     * @param startTime 开始时间
     * @param days      间隔天数
     * @return stopTime ：结束时间
     */
    public static String upToDay2(String startTime, int days) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        long time = 0;
        Calendar calendar = Calendar.getInstance();
        try {
            Date startDate = simpleDateFormat.parse(startTime);

            calendar.setTime(startDate);
            calendar.add(Calendar.DATE, days);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date stopDate = calendar.getTime();
        String stopTime = simpleDateFormat.format(stopDate);
        return stopTime;
    }


    public static String endToDay(String startTime, int days) {
        int day = days - 1;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        long time = 0;
        Calendar calendar = Calendar.getInstance();
        try {
            Date startDate = simpleDateFormat.parse(startTime);

            calendar.setTime(startDate);
            calendar.add(Calendar.DATE, days);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date stopDate = calendar.getTime();
        String stopTime = simpleDateFormat.format(stopDate);
        return stopTime;
    }

    /**
     * 判断两日期之间的天数是否符合传入的天数
     *
     * @param start   开始时间
     * @param end     结束时间
     * @param daygaps 传入天数
     * @return
     * @throws ParseException
     */
    public static boolean isConfirmToDate(String start, String end, String daygaps) throws
            ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        Date startDate = simpleDateFormat.parse(start);
        Date endDate = simpleDateFormat.parse(end);
        long startLong = startDate.getTime();
        long endLong = endDate.getTime();

        long daylong = endLong - startLong;
        int day = (int) (daylong / (24 * 60 * 60 * 1000));
        int gap = Integer.parseInt(daygaps);
        if (day == gap)
            return true;
        else
            return false;

    }


    /**
     * 判断一个时间在另一个之后
     *
     * @param startTime
     * @param endTime
     * @return
     */
    public static boolean dateAfterDate_UTC(String startTime, String endTime) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date startDate = format.parse(startTime);
            Date endDate = format.parse(endTime);
            long start = startDate.getTime();
            long end = endDate.getTime();
            if (end > start)
                return true;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * 判断一个时间在另一个之后
     *
     * @param startTime
     * @param endTime
     * @return
     */
    public static boolean dateAfterDate_V2(String startTime, String endTime) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日");
        try {
            Date startDate = format.parse(startTime);
            Date endDate = format.parse(endTime);
            long start = startDate.getTime();
            long end = endDate.getTime();
            if (end >= start)
                return true;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }


    public static boolean dateAfterDate(String startTime, String endTime, @NonNull String fs) {
        SimpleDateFormat format = new SimpleDateFormat(fs);
        try {
            Date startDate = format.parse(startTime);
            Date endDate = format.parse(endTime);
            long start = startDate.getTime();
            long end = endDate.getTime();
            if (end > start)
                return true;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * 下一天时间
     *
     * @param time 2016年9月2日
     * @return 2016年9月3日
     */
    public static String nextDayDate(String time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        Calendar calendar = Calendar.getInstance();
        String next = "";
        try {
            Date currentDay = simpleDateFormat.parse(time);
            calendar.setTime(currentDay);
            calendar.add(Calendar.DATE, 1);
            next = simpleDateFormat.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return next;
    }


    public static String forwardDate(String time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
        Calendar calendar = Calendar.getInstance();
        String forward = "";
        try {
            Date currentDay = simpleDateFormat.parse(time);
            calendar.setTime(currentDay);
            calendar.add(Calendar.DATE, -1);
            forward = simpleDateFormat.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return forward;
    }

    public static String forwardDate2(String time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        String forward = "";
        try {
            Date currentDay = simpleDateFormat.parse(time);
            calendar.setTime(currentDay);
            calendar.add(Calendar.DATE, -1);
            forward = simpleDateFormat.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return forward;
    }


    /**
     * 从yyyy年MM月dd日转换到yyyy-MM-dd
     *
     * @param time
     * @return
     */
    public static String getEncodeDate(String time) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy年MM月dd日");
        String resultTime = "";
        try {
            Date date = dateFormat2.parse(time);
            resultTime = dateFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return resultTime;

    }

    //get the normal time by format the service time
    //there are two way to format the service,one is UTC ,two is GMT
    public static String getResultTime(String time) {
        SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        SimpleDateFormat dateFormatGMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String resultTime = "";
        try {
            if (time.length() > 19) {
                Date date = dateFormatUTC.parse(time);
                resultTime = dateFormat.format(date);
            } else {
                Date date = dateFormatGMT.parse(time);
                resultTime = dateFormat.format(date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultTime;

    }

    public static String getCurrentUTCtime() {
        SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        Date date = new Date(System.currentTimeMillis());
        return dateFormatUTC.format(date);
    }

    /**
     * 明天日期
     *
     * @return
     */
    public static String getNextTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        Calendar calendar = Calendar.getInstance();
        String next = "";
        Date date = new Date();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, 1);
        next = simpleDateFormat.format(calendar.getTime());

        return next;
    }

    /**
     * 获取昨天日期
     */
    public static String getYesterdayTime(Boolean isEnd) {
        SimpleDateFormat simpleDateFormat;
        if (isEnd) {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        } else {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        }
        Calendar calendar = Calendar.getInstance();
        String next = "";
        Date date = new Date();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, -1);
        next = simpleDateFormat.format(calendar.getTime());
        return next;
    }

    public static Date stringToDate(String dateStr) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date = format.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static Long stringToTime(String dateStr) {
        Date date = null;
        if (!TextUtils.isEmpty(dateStr)) {
            date = stringToDate(dateStr);
        } else {
            date = new Date();
        }
        return date.getTime();
    }

    public static String dateToString(Date date, String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        return simpleDateFormat.format(date);
    }

    public static String dateToFormat(int year, int month, int day) {
        return year + "-" + (month < 10 ? "0" + month : month) + "-" + (day < 10 ? "0" + day : day);
    }

}
