#pragma once


#include <atomic>
#include <memory>
#include <array>

namespace ircamera {

    template<typename T, size_t Size = 1024>
    class lockfree_queue {
        static_assert((Size
        & (Size - 1)) == 0, "Size must be power of 2");
        static_assert(Size
        > 1, "Size must be greater than 1");

    public:

        lockfree_queue() : head_(0), tail_(0) {

            for (size_t i = 0; i < Size; ++i) {
                slots_[i].sequence.store(i, std::memory_order_relaxed);
            }
        }

        bool try_push(const T &item) {
            return emplace(item);
        }

        bool try_push(T &&item) {
            return emplace(std::move(item));
        }

        template<typename... Args>
        bool emplace(Args &&... args) {
            const size_t head = head_.load(std::memory_order_relaxed);
            slot_t &slot = slots_[head & mask_];

            if (slot.sequence.load(std::memory_order_acquire) != head) {
                return false;  // Queue is full
            }

            new(&slot.item) T(std::forward<Args>(args)...);

            slot.sequence.store(head + 1, std::memory_order_release);
            head_.store(head + 1, std::memory_order_relaxed);

            return true;
        }

        bool try_pop(T &item) {
            const size_t tail = tail_.load(std::memory_order_relaxed);
            slot_t &slot = slots_[tail & mask_];

            if (slot.sequence.load(std::memory_order_acquire) != tail + 1) {
                return false;  // Queue is empty
            }

            item = std::move(slot.item);

            slot.item.~T();

            slot.sequence.store(tail + mask_ + 1, std::memory_order_release);
            tail_.store(tail + 1, std::memory_order_relaxed);

            return true;
        }

        size_t size() const {
            const size_t head = head_.load(std::memory_order_relaxed);
            const size_t tail = tail_.load(std::memory_order_relaxed);
            return head - tail;
        }

        bool empty() const {
            return size() == 0;
        }

        bool full() const {
            return size() >= Size - 1;
        }

        constexpr size_t

        capacity() const {
            return Size;
        }

        void clear() {
            T item;
            while (try_pop(item)) {

            }
        }

    private:
        static constexpr size_t
        mask_ = Size - 1;

        struct slot_t {
            std::atomic <size_t> sequence;
            alignas(T)
            char storage[sizeof(T)];

            T &item{
                    return reinterpret_cast<T&>(storage);
            }

            const T &item const {
                return reinterpret_cast<const T &>(storage);
            }
        };

        alignas(64)
        std::atomic <size_t> head_;
        alignas(64)
        std::atomic <size_t> tail_;
        alignas(64)
        std::array <slot_t, Size> slots_;
    };

    template<typename T, size_t Size = 1024>
    class mpsc_lockfree_queue {
        static_assert((Size
        & (Size - 1)) == 0, "Size must be power of 2");

    public:
        mpsc_lockfree_queue() : head_(0), tail_(0) {}

        bool try_push(const T &item) {
            const size_t head = head_.fetch_add(1, std::memory_order_relaxed);
            const size_t index = head & mask_;

            while (slots_[index].sequence.load(std::memory_order_acquire) != head) {
                std::this_thread::yield();
            }

            slots_[index].item = item;
            slots_[index].sequence.store(head + 1, std::memory_order_release);

            return true;
        }

        bool try_pop(T &item) {
            const size_t tail = tail_.load(std::memory_order_relaxed);
            const size_t index = tail & mask_;

            if (slots_[index].sequence.load(std::memory_order_acquire) != tail + 1) {
                return false;
            }

            item = std::move(slots_[index].item);
            slots_[index].sequence.store(tail + Size, std::memory_order_release);
            tail_.store(tail + 1, std::memory_order_relaxed);

            return true;
        }

        size_t size() const {
            const size_t head = head_.load(std::memory_order_relaxed);
            const size_t tail = tail_.load(std::memory_order_relaxed);
            return head - tail;
        }

        bool empty() const { return size() == 0; }

        constexpr size_t

        capacity() const { return Size; }

    private:
        static constexpr size_t
        mask_ = Size - 1;

        struct slot_t {
            std::atomic <size_t> sequence{0};
            T item;
        };

        alignas(64)
        std::atomic <size_t> head_;
        alignas(64)
        std::atomic <size_t> tail_;
        alignas(64)
        std::array <slot_t, Size> slots_;
    };

} // namespace ircamera
