package com.ssafy.s15p21a206.tiger.feature.session

import java.text.DateFormat
import java.util.Date

/**
 * 수집 시각을 화면 문구로 옮긴다. 목록 카드, 상세 제목, 상세 정보 시트가 같은 형식을 쓴다.
 *
 * 형식은 기기의 언어 설정을 따르는 날짜·시각 기본형이다.
 */
internal fun formatCaptureTime(epochMs: Long): String = DateFormat.getDateTimeInstance().format(Date(epochMs))
