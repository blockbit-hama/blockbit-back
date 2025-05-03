package com.sg.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object DateTimeUtil {
    
    /**
     * 현재 날짜를 'yyyyMMdd' 형식의 문자열로 반환합니다.
     */
    fun getCurrentDate(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    }
    
    /**
     * 현재 시간을 'HHmmss' 형식의 문자열로 반환합니다.
     */
    fun getCurrentTime(): String {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
    }
    
    /**
     * 문자열 형태의 날짜를 LocalDate 객체로 변환합니다.
     * @param dateStr 'yyyyMMdd' 형식의 날짜 문자열
     */
    fun parseDate(dateStr: String): LocalDate {
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"))
    }
    
    /**
     * 문자열 형태의 시간을 LocalTime 객체로 변환합니다.
     * @param timeStr 'HHmmss' 형식의 시간 문자열
     */
    fun parseTime(timeStr: String): LocalTime {
        return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HHmmss"))
    }
    
    /**
     * 날짜와 시간 문자열을 결합하여 LocalDateTime 객체로 변환합니다.
     * @param dateStr 'yyyyMMdd' 형식의 날짜 문자열
     * @param timeStr 'HHmmss' 형식의 시간 문자열
     */
    fun parseDateTime(dateStr: String, timeStr: String): LocalDateTime {
        val date = parseDate(dateStr)
        val time = parseTime(timeStr)
        return LocalDateTime.of(date, time)
    }
    
    /**
     * LocalDateTime 객체에서 'yyyyMMdd' 형식의 날짜 문자열을 추출합니다.
     */
    fun extractDateString(dateTime: LocalDateTime): String {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    }
    
    /**
     * LocalDateTime 객체에서 'HHmmss' 형식의 시간 문자열을 추출합니다.
     */
    fun extractTimeString(dateTime: LocalDateTime): String {
        return dateTime.format(DateTimeFormatter.ofPattern("HHmmss"))
    }
}
