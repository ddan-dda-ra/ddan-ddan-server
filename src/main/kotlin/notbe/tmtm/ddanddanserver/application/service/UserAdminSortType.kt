package notbe.tmtm.ddanddanserver.application.service

import org.springframework.data.domain.Sort

enum class UserAdminSortType(
    val sort: Sort,
) {
    /** 최근 로그인 순 (default) */
    LATEST_LOGIN(Sort.by(Sort.Direction.DESC, "lastLoginAt")),

    /** 가입 순 (최신 가입 우선) — _id의 ObjectId timestamp 기반 */
    JOINED(Sort.by(Sort.Direction.DESC, "_id")),

    /** 최초 가입 순 (오래된 가입 우선) */
    JOINED_ASC(Sort.by(Sort.Direction.ASC, "_id")),
    ;
}
