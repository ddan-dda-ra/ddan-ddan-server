package notbe.tmtm.ddanddanserver.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import notbe.tmtm.ddanddanserver.application.service.CheerService
import notbe.tmtm.ddanddanserver.common.WebExceptionHandler
import notbe.tmtm.ddanddanserver.domain.exception.CheerAlreadyExistsException
import notbe.tmtm.ddanddanserver.domain.exception.CheerNotFriendsException
import notbe.tmtm.ddanddanserver.domain.exception.CheerSelfException
import notbe.tmtm.ddanddanserver.domain.model.cheer.Cheer
import org.bson.types.ObjectId
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class CheerControllerTest : FunSpec({
    lateinit var cheerService: CheerService
    lateinit var cheerController: CheerController
    lateinit var mockMvc: MockMvc
    lateinit var objectMapper: ObjectMapper

    val testUserId = ObjectId()
    val testFriendId = ObjectId()

    beforeEach {
        cheerService = mockk()
        cheerController = CheerController(cheerService)
        objectMapper = ObjectMapper().findAndRegisterModules()

        mockMvc = MockMvcBuilders
            .standaloneSetup(cheerController)
            .setControllerAdvice(WebExceptionHandler())
            .build()

        // Mock authentication
        val authentication = UsernamePasswordAuthenticationToken(testUserId.toString(), null, emptyList())
        SecurityContextHolder.getContext().authentication = authentication
    }

    afterEach {
        SecurityContextHolder.clearContext()
    }

    context("POST /v1/cheers/{friendId}") {
        test("정상적으로 응원을 생성한다") {
            // given
            val expectedCheer = Cheer.create(testUserId, testFriendId)
            every { cheerService.createCheer(testUserId, testFriendId) } returns expectedCheer

            // when & then
            mockMvc.perform(
                post("/v1/cheers/{friendId}", testFriendId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .principal(UsernamePasswordAuthenticationToken(testUserId.toString(), null, emptyList()))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.cheerId").value(expectedCheer.id.toString()))
                .andExpect(jsonPath("$.cheereeId").value(testFriendId.toString()))
                .andExpect(jsonPath("$.date").exists())
                .andExpect(jsonPath("$.createdAt").exists())
        }

        test("친구가 아닌 경우 400 에러를 반환한다") {
            // given
            every { cheerService.createCheer(testUserId, testFriendId) } throws CheerNotFriendsException()

            // when & then
            mockMvc.perform(
                post("/v1/cheers/{friendId}", testFriendId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .principal(UsernamePasswordAuthenticationToken(testUserId.toString(), null, emptyList()))
            )
                .andExpect(status().isBadRequest)
        }

        test("자기 자신을 응원하려 할 때 400 에러를 반환한다") {
            // given
            every { cheerService.createCheer(testUserId, testUserId) } throws CheerSelfException()

            // when & then
            mockMvc.perform(
                post("/v1/cheers/{friendId}", testUserId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .principal(UsernamePasswordAuthenticationToken(testUserId.toString(), null, emptyList()))
            )
                .andExpect(status().isBadRequest)
        }

        test("중복 응원 시도 시 409 에러를 반환한다") {
            // given
            every { cheerService.createCheer(testUserId, testFriendId) } throws CheerAlreadyExistsException()

            // when & then
            mockMvc.perform(
                post("/v1/cheers/{friendId}", testFriendId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .principal(UsernamePasswordAuthenticationToken(testUserId.toString(), null, emptyList()))
            )
                .andExpect(status().isConflict)
        }

        test("잘못된 친구 ID 형식일 때 400 에러를 반환한다") {
            // when & then
            mockMvc.perform(
                post("/v1/cheers/{friendId}", "invalid-object-id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .principal(UsernamePasswordAuthenticationToken(testUserId.toString(), null, emptyList()))
            )
                .andExpect(status().isBadRequest)
        }
    }

})
