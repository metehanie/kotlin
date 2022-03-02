/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>
#include <iterator>
#include <limits>
#if __has_include(<variant>)
#include <variant>
#endif

#include "KAssert.h"
#include "Utils.hpp"

namespace kotlin {

template <typename T>
struct DefaultIntrusiveForwardListTraits {
    static T* next(const T& value) { return value.next(); }

    static void setNext(T& value, T* next) { value.setNext(next); }
};

// Intrusive variant of `std::forward_list`.
// The container does not own nodes. Care must be taken not to allow a node
// to be in two containers at once.
// Unlike `std::forward_list` this has a special `const_before_begin_iterator` that is
// used as a placeholder "iterator" before the `begin()`. It is not, however, an iterator, because
// it's impossible to implement `operator++` on it.
// There's no guarantee of clearing the next pointer when a node leaves the container.
template <typename T, typename Traits = DefaultIntrusiveForwardListTraits<T>>
class intrusive_forward_list : private MoveOnly {
public:
    using value_type = T;
    using size_type = size_t;
    using difference_type = ptrdiff_t;
    using reference = value_type&;
    using const_reference = const value_type&;
    using pointer = value_type*;
    using const_pointer = const value_type*;

    class const_before_begin_iterator {
    public:
        bool operator==(const const_before_begin_iterator& rhs) const noexcept { return true; }
        bool operator!=(const const_before_begin_iterator& rhs) const noexcept { return !(*this == rhs); }
    };

    class iterator {
    public:
        using difference_type = intrusive_forward_list::difference_type;
        using value_type = intrusive_forward_list::value_type;
        using pointer = intrusive_forward_list::pointer;
        using reference = intrusive_forward_list::reference;
        using iterator_category = std::forward_iterator_tag;

        iterator() noexcept = default;
        iterator(const iterator&) noexcept = default;
        iterator& operator=(const iterator&) noexcept = default;

        reference operator*() noexcept { return *node_; }

        pointer operator->() noexcept { return node_; }

        iterator& operator++() noexcept {
            node_ = next(node_);
            return *this;
        }

        iterator operator++(int) noexcept {
            auto result = *this;
            ++(*this);
            return result;
        }

        bool operator==(const iterator& rhs) const noexcept { return node_ == rhs.node_; }
        bool operator!=(const iterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        friend class intrusive_forward_list;

        explicit iterator(pointer node) noexcept : node_(node) {}

        intrusive_forward_list::pointer node_ = nullptr;
    };

    class const_iterator {
    public:
        using difference_type = intrusive_forward_list::difference_type;
        using value_type = const intrusive_forward_list::value_type;
        using pointer = intrusive_forward_list::const_pointer;
        using reference = intrusive_forward_list::const_reference;
        using iterator_category = std::forward_iterator_tag;

        const_iterator() noexcept = default;
        const_iterator(const const_iterator&) noexcept = default;
        const_iterator& operator=(const const_iterator&) noexcept = default;

        const_iterator(iterator it) noexcept : node_(it.node_) {}

        reference operator*() noexcept { return *node_; }

        pointer operator->() noexcept { return node_; }

        const_iterator& operator++() noexcept {
            node_ = next(node_);
            return *this;
        }

        const_iterator operator++(int) noexcept {
            auto result = *this;
            ++(*this);
            return result;
        }

        bool operator==(const const_iterator& rhs) const noexcept { return node_ == rhs.node_; }
        bool operator!=(const const_iterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        friend class intrusive_forward_list;

        explicit const_iterator(intrusive_forward_list::pointer node) noexcept : node_(node) {}

        intrusive_forward_list::pointer node_ = nullptr;
    };

    intrusive_forward_list() noexcept = default;

    intrusive_forward_list(intrusive_forward_list&& rhs) noexcept : head_(rhs.head_) { rhs.head_ = nullptr; }

    template <typename InputIt>
    intrusive_forward_list(InputIt first, InputIt last) noexcept {
        assign(std::move(first), std::move(last));
    }

    ~intrusive_forward_list() = default;

    intrusive_forward_list& operator=(intrusive_forward_list&& rhs) noexcept {
        intrusive_forward_list tmp(std::move(rhs));
        swap(tmp);
        return *this;
    }

    void swap(intrusive_forward_list& rhs) noexcept {
        using std::swap;
        swap(head_, rhs.head_);
    }

    template <typename InputIt>
    void assign(InputIt first, InputIt last) noexcept {
        clear();
        if (first != last) {
            insert_after(cbefore_begin(), std::move(first), std::move(last));
        }
    }

    reference front() noexcept { return *head_; }
    const_reference front() const noexcept { return *head_; }

    const_before_begin_iterator before_begin() const noexcept { return const_before_begin_iterator{}; }
    const_before_begin_iterator cbefore_begin() const noexcept { return const_before_begin_iterator{}; }

    iterator begin() noexcept { return iterator(head_); }
    const_iterator begin() const noexcept { return const_iterator(head_); }
    const_iterator cbegin() const noexcept { return const_iterator(head_); }

    iterator end() noexcept { return iterator(); }
    const_iterator end() const noexcept { return const_iterator(); }
    const_iterator cend() const noexcept { return const_iterator(); }

    bool empty() const noexcept { return head_ == nullptr; }

    size_type max_size() const noexcept { return std::numeric_limits<size_type>::max(); }

    void clear() noexcept { head_ = nullptr; }

    iterator insert_after(const_iterator pos, reference value) noexcept {
        pointer nextNode = next(pos.node_);
        setNext(pos.node_, &value);
        setNext(&value, nextNode);
        return iterator(&value);
    }

    iterator insert_after(const_before_begin_iterator pos, reference value) noexcept {
        setNext(&value, head_);
        head_ = &value;
        return begin();
    }

    template <typename InputIt>
    iterator insert_after(const_iterator pos, InputIt first, InputIt last) noexcept {
        pointer nextNode = next(pos.node_);
        pointer prevNode = pos.node_;
        for (auto it = first; it != last; ++it) {
            setNext(prevNode, &*it);
            prevNode = &*it;
        }
        setNext(prevNode, nextNode);
        return iterator(prevNode);
    }

#if __has_include(<variant>)
    // Unlike `std::forward_list` this cannot return `iterator`, because for empty `(first, last)`
    // interval, this has to return `before_begin()`, and for non-empty intervals: a regular `iterator`.
    template <typename InputIt>
    std::variant<iterator, const_before_begin_iterator> insert_after(
            const_before_begin_iterator pos, InputIt first, InputIt last) noexcept {
        if (first == last) {
            return pos;
        }
        reference value = *first;
        ++first;
        auto it = insert_after(pos, value);
        return insert_after(it, std::move(first), std::move(last));
    }
#endif

    iterator erase_after(const_iterator pos) noexcept {
        pointer prevNode = pos.node_;
        pointer nodeToErase = next(pos.node_);
        if (!nodeToErase) {
            return end();
        }
        pointer nextNode = next(nodeToErase);
        setNext(prevNode, nextNode);
        setNext(nodeToErase, nullptr);
        return iterator(nextNode);
    }

    iterator erase_after(const_before_begin_iterator pos) noexcept {
        if (head_) {
            head_ = next(head_);
        }
        return begin();
    }

    iterator erase_after(const_iterator first, const_iterator last) noexcept {
        setNext(first.node_, last.node_);
        return iterator(last.node_);
    }

    iterator erase_after(const_before_begin_iterator pos, const_iterator last) noexcept {
        head_ = last.node_;
        return begin();
    }

    void push_front(reference value) noexcept { insert_after(cbefore_begin(), value); }

    void pop_front() noexcept { erase_after(cbefore_begin()); }

    void splice_after(const_iterator pos, intrusive_forward_list& rhs) noexcept { splice_after(pos, rhs, rhs.cbefore_begin(), rhs.cend()); }

    void splice_after(const_before_begin_iterator pos, intrusive_forward_list& rhs) noexcept {
        splice_after(pos, rhs, rhs.cbefore_begin(), rhs.cend());
    }

    void splice_after(const_iterator pos, intrusive_forward_list&& rhs) noexcept {
        intrusive_forward_list other(std::move(rhs));
        splice_after(pos, other);
    }

    void splice_after(const_before_begin_iterator pos, intrusive_forward_list&& rhs) noexcept {
        intrusive_forward_list other(std::move(rhs));
        splice_after(pos, other);
    }

    void splice_after(const_iterator pos, intrusive_forward_list& rhs, const_iterator it) noexcept {
        splice_after(pos, rhs, it, std::next(it, 2));
    }

    void splice_after(const_before_begin_iterator pos, intrusive_forward_list& rhs, const_iterator it) noexcept {
        splice_after(pos, rhs, it, std::next(it, 2));
    }

    void splice_after(const_iterator pos, intrusive_forward_list&& rhs, const_iterator it) noexcept {
        intrusive_forward_list other(std::move(rhs));
        splice_after(pos, other, it);
    }

    void splice_after(const_before_begin_iterator pos, intrusive_forward_list&& rhs, const_iterator it) noexcept {
        intrusive_forward_list other(std::move(rhs));
        splice_after(pos, other, it);
    }

    void splice_after(const_iterator pos, intrusive_forward_list& rhs, const_before_begin_iterator it) noexcept {
        splice_after(pos, rhs, it, std::next(rhs.begin(), 1));
    }

    void splice_after(const_before_begin_iterator pos, intrusive_forward_list& rhs, const_before_begin_iterator it) noexcept {
        splice_after(pos, rhs, it, std::next(rhs.begin(), 1));
    }

    void splice_after(const_iterator pos, intrusive_forward_list&& rhs, const_before_begin_iterator it) noexcept {
        intrusive_forward_list other(std::move(rhs));
        splice_after(pos, other, it);
    }

    void splice_after(const_before_begin_iterator pos, intrusive_forward_list&& rhs, const_before_begin_iterator it) noexcept {
        intrusive_forward_list other(std::move(rhs));
        splice_after(pos, other, it);
    }

    void splice_after(const_iterator pos, intrusive_forward_list& rhs, const_iterator first, const_iterator last) noexcept {
        pointer thisBeforeRange = pos.node_;
        pointer thisAfterRange = next(thisBeforeRange);
        pointer rhsBeforeRange = first.node_;
        pointer rhsAfterRange = last.node_;
        pointer rangeFirst = next(rhsBeforeRange);
        pointer rangeLast = previous(rangeFirst, rhsAfterRange);
        if (!rangeFirst) {
            // Empty range.
            return;
        }
        setNext(thisBeforeRange, rangeFirst);
        setNext(rangeLast, thisAfterRange);
        setNext(rhsBeforeRange, rhsAfterRange);
    }

    void splice_after(const_before_begin_iterator pos, intrusive_forward_list& rhs, const_iterator first, const_iterator last) noexcept {
        pointer thisAfterRange = head_;
        pointer rhsBeforeRange = first.node_;
        pointer rhsAfterRange = last.node_;
        pointer rangeFirst = next(rhsBeforeRange);
        pointer rangeLast = previous(rangeFirst, rhsAfterRange);
        if (!rangeFirst) {
            // Empty range.
            return;
        }
        head_ = rangeFirst;
        setNext(rangeLast, thisAfterRange);
        setNext(rhsBeforeRange, rhsAfterRange);
    }

    void splice_after(const_iterator pos, intrusive_forward_list&& rhs, const_iterator first, const_iterator last) noexcept {
        intrusive_forward_list other(std::move(rhs));
        splice_after(pos, other, first, last);
    }

    void splice_after(const_before_begin_iterator pos, intrusive_forward_list&& rhs, const_iterator first, const_iterator last) noexcept {
        intrusive_forward_list other(std::move(rhs));
        splice_after(pos, other, first, last);
    }

    void splice_after(const_iterator pos, intrusive_forward_list& rhs, const_before_begin_iterator first, const_iterator last) noexcept {
        pointer thisBeforeRange = pos.node_;
        pointer thisAfterRange = next(thisBeforeRange);
        pointer rhsAfterRange = last.node_;
        pointer rangeFirst = rhs.head_;
        pointer rangeLast = previous(rangeFirst, rhsAfterRange);
        if (!rangeFirst) {
            // Empty range.
            return;
        }
        setNext(thisBeforeRange, rangeFirst);
        setNext(rangeLast, thisAfterRange);
        rhs.head_ = rhsAfterRange;
    }

    void splice_after(
            const_before_begin_iterator pos, intrusive_forward_list& rhs, const_before_begin_iterator first, const_iterator last) noexcept {
        pointer thisAfterRange = head_;
        pointer rhsAfterRange = last.node_;
        pointer rangeFirst = rhs.head_;
        pointer rangeLast = previous(rangeFirst, rhsAfterRange);
        if (!rangeFirst) {
            return;
        }
        head_ = rangeFirst;
        setNext(rangeLast, thisAfterRange);
        rhs.head_ = rhsAfterRange;
    }

    void splice_after(const_iterator pos, intrusive_forward_list&& rhs, const_before_begin_iterator first, const_iterator last) noexcept {
        intrusive_forward_list other(std::move(rhs));
        splice_after(pos, other, first, last);
    }

    void splice_after(
            const_before_begin_iterator pos,
            intrusive_forward_list&& rhs,
            const_before_begin_iterator first,
            const_iterator last) noexcept {
        intrusive_forward_list other(std::move(rhs));
        splice_after(pos, other, first, last);
    }

    void remove(reference value) noexcept {
        // TODO: no need to move on after finding the first match.
        return remove_if([&value](const_reference x) { return &x == &value; });
    }

    template <typename P>
    void remove_if(P p) noexcept {
        // Determine the new head first.
        pointer newHead = head_;
        for (; newHead && p(*newHead); newHead = next(newHead)) {
        }
        head_ = newHead;
        pointer prev = newHead;
        if (!prev) {
            return;
        }
        pointer node = next(prev);
        while (node) {
            if (p(*node)) {
                // The node is being removed.
                node = next(node);
                setNext(prev, node);
            } else {
                // The node is staying.
                prev = node;
                node = next(node);
            }
        }
    }

private:
    static pointer next(const_pointer node) noexcept { return Traits::next(*node); }

    static void setNext(pointer node, pointer next) noexcept { return Traits::setNext(*node, next); }

    static pointer previous(pointer begin, pointer end) noexcept {
        pointer prev = nullptr;
        for (auto it = begin; it != end; it = next(it)) {
            prev = it;
        }
        return prev;
    }

    pointer head_ = nullptr;
};

template <typename InputIt>
intrusive_forward_list(InputIt, InputIt) -> intrusive_forward_list<typename std::iterator_traits<InputIt>::value_type>;

} // namespace kotlin
