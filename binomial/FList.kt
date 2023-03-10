package binomial

/*
 * FList - реализация функционального списка
 *
 * Пустому списку соответствует тип Nil, непустому - Cons
 *
 * Запрещено использовать
 *
 *  - var
 *  - циклы
 *  - стандартные коллекции
 *
 *  Исключение Array-параметр в функции flistOf. Но даже в ней нельзя использовать цикл и forEach.
 *  Только обращение по индексу
 */
sealed class FList<T> : Iterable<T> {
    // размер списка, 0 для Nil, количество элементов в цепочке для Cons
    abstract val size: Int

    // пустой ли списк, true для Nil, false для Cons
    abstract val isEmpty: Boolean

    // получить список, применив преобразование
    // требуемая сложность - O(n)
    abstract fun <U> map(f: (T) -> U): FList<U>

    // получить список из элементов, для которых f возвращает true
    // требуемая сложность - O(n)
    abstract fun filter(f: (T) -> Boolean): FList<T>

    // свертка
    // требуемая сложность - O(n)
    // Для каждого элемента списка (curr) вызываем f(acc, curr),
    // где acc - это base для начального элемента, или результат вызова
    // f(acc, curr) для предыдущего
    // Результатом fold является результат последнего вызова f(acc, curr)
    // или base, если список пуст
    abstract fun <U> fold(base: U, f: (U, T) -> U): U

    // разворот списка
    // требуемая сложность - O(n)
    fun reverse(): FList<T> = fold<FList<T>>(nil()) { acc, current ->
        Cons(current, acc)
    }

    /*
     * Это не очень красиво, что мы заводим отдельный Nil на каждый тип
     * И вообще лучше, чтобы Nil был объектом
     *
     * Но для этого нужны приседания с ковариантностью
     *
     * dummy - костыль для того, что бы все Nil-значения были равны
     *         и чтобы Kotlin-компилятор был счастлив (он требует, чтобы у Data-классов
     *         были свойство)
     *
     * Также для борьбы с бойлерплейтом были введены функция и свойство nil в компаньоне
     */
    data class Nil<T>(private val dummy: Int = 0) : FList<T>() {
        override val size: Int = 0

        override val isEmpty: Boolean = true

        override fun <U> fold(base: U, f: (U, T) -> U): U = base

        override fun filter(f: (T) -> Boolean): FList<T> = nil()

        override fun <U> map(f: (T) -> U): FList<U> = nil()

        override fun iterator(): Iterator<T> = object : Iterator<T> {
            override fun next(): Nothing = throw NoSuchElementException("Nil has no elements")
            override fun hasNext(): Boolean = false
        }
    }

    data class Cons<T>(val head: T, val tail: FList<T>) : FList<T>() {
        override val size: Int
            get() = 1 + tail.size
        override val isEmpty: Boolean
            get() = false

        override fun <U> fold(base: U, f: (U, T) -> U): U {
            tailrec fun recFold(acc: U, tail: Cons<T>): U =
                if (tail.size == 1) f(acc, tail.head) else recFold(f(acc, tail.head), tail.tail as Cons<T>)
            return recFold(base, this)
        }

        override fun filter(f: (T) -> Boolean): FList<T> {
            tailrec fun recFilter(fList: FList<T>, it: Iterator<T>) : FList<T> {
                if (!it.hasNext()) return fList
                val next = it.next()
                return if (f(next)) recFilter(Cons(next, fList), it) else recFilter(fList, it)
            }
            return recFilter(nil(), iterator()).reverse()
        }

        override fun <U> map(f: (T) -> U): FList<U> {
            tailrec fun recMap(fList: FList<U>, it: Iterator<T>) : FList<U> =
                if (!it.hasNext()) fList
                else recMap(Cons(f(it.next()), fList), it)

            return recMap(nil(), iterator()).reverse()
        }

        override fun iterator(): Iterator<T> = object : Iterator<T> {
            var curr: FList<T> = this@Cons
            override fun hasNext(): Boolean = !curr.isEmpty

            override fun next(): T {
                if (hasNext()) {
                    val ret = (curr as Cons).head
                    curr = (curr as Cons).tail
                    return ret
                } else {
                    throw NoSuchElementException("End has been reached")
                }
            }
        }
    }

    companion object {
        fun <T> nil() = Nil<T>()
        val nil = Nil<Any>()
    }
}

// конструирование функционального списка в порядке следования элементов
// требуемая сложность - O(n)
fun <T> flistOf(vararg values: T): FList<T> {
    tailrec fun recFlistOf(fList: FList<T>, vararg values: T) : FList<T> =
        if (values.isEmpty()) fList
        else recFlistOf(FList.Cons(values.first(), fList), *values.sliceArray(1 until values.size))

    return recFlistOf(FList.Nil(), *values)
}
