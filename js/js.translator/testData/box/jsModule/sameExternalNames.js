$kotlin_test_internal$.beginModule();

function A(x) {
    this.x = x;
}

module.exports = A;

$kotlin_test_internal$.endModule("foo");

$kotlin_test_internal$.beginModule();

function B(x) {
    this.x = x;
}

module.exports = B

$kotlin_test_internal$.endModule("bar");