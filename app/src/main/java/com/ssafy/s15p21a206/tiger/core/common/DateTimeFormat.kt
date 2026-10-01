package com.ssafy.s15p21a206.tiger.core.common

import java.text.DateFormat
import java.util.Date

/**
 * 시각을 화면 문구로 옮긴다. 형식은 기기의 언어 설정을 따르는 날짜·시각 기본형이다.
 *
 * 화면마다 형식을 따로 고르면 같은 시각이 자리마다 다른 모양으로 보이므로 시각은 모두 이것으로 보인다.
 */
internal fun formatDateTime(epochMs: Long): String = DateFormat.getDateTimeInstance().format(Date(epochMs))
