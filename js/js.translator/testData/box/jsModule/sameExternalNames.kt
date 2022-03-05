// MODULE_KIND: COMMON_JS

// FILE: a.kt
package foo

@JsModule("foo")
external class A(x: Int) {
    val x: Int
}

// FILE: b.kt
package bar

@JsModule("bar")
external class A(x: Int) {
    val x: Int
}

// FILE: main.kt
import foo.A
import bar.A as B

fun box(): String {
    val a = A(37)
    val b = B(73)
    assertEquals(37, a.x)
    assertEquals(73, b.x)

    return "OK"
}