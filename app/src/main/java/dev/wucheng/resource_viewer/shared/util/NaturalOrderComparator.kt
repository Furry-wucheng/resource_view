package dev.wucheng.resource_viewer.shared.util

object NaturalOrderComparator : Comparator<String> {

    override fun compare(a: String, b: String): Int {
        var ia = 0
        var ib = 0
        while (ia < a.length && ib < b.length) {
            val ca = a[ia]
            val cb = b[ib]
            if (ca.isDigit() && cb.isDigit()) {
                var na = 0L
                while (ia < a.length && a[ia].isDigit()) {
                    na = na * 10 + (a[ia] - '0').toLong()
                    ia++
                }
                var nb = 0L
                while (ib < b.length && b[ib].isDigit()) {
                    nb = nb * 10 + (b[ib] - '0').toLong()
                    ib++
                }
                if (na != nb) return na.compareTo(nb)
            } else {
                val lowerA = ca.lowercaseChar()
                val lowerB = cb.lowercaseChar()
                if (lowerA != lowerB) return lowerA.compareTo(lowerB)
                ia++
                ib++
            }
        }
        return (a.length - ia).compareTo(b.length - ib)
    }
}
