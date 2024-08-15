package notbe.tmtm.ddanddanserver.domain.usecase

fun interface UseCase<I, O> {
    fun execute(input: I): O
}
