

#pragma once

#include <atomic>
#include <memory>
#include <vector>

namespace ircamera {
    namespace utils {

        template<typename T>
        class ThreadSafeQueue {
        public:
            ThreadSafeQueue(size_t capacity = 10000);

            ~ThreadSafeQueue() = default;

            ThreadSafeQueue(const ThreadSafeQueue &) = delete;

            ThreadSafeQueue &operator=(const ThreadSafeQueue &) = delete;

            ThreadSafeQueue(ThreadSafeQueue &&) noexcept = default;

            ThreadSafeQueue &operator=(ThreadSafeQueue &&) noexcept = default;

            bool push(const T &item);

            bool push(T &&item);

            bool pop(T &item);

            size_t size() const;

            bool empty() const;

            bool full() const;

            void clear();

            size_t capacity() const {
                return capacity_;
            }

        private:
            struct Node {
                std::atomic<T *> data{nullptr};
                std::atomic<Node *> next{nullptr};
            };

            std::atomic<Node *> head_;
            std::atomic<Node *> tail_;
            std::atomic <size_t> size_;
            const size_t capacity_;

            std::vector <std::unique_ptr<Node>> node_pool_;
            std::atomic <size_t> pool_index_;
        };

        template<typename T>
        ThreadSafeQueue<T>::ThreadSafeQueue(size_t capacity)
                : capacity_(capacity), size_(0), pool_index_(0) {

            node_pool_.reserve(capacity + 1);
            for (size_t i = 0; i <= capacity; ++i) {
                node_pool_.emplace_back(std::make_unique<Node>());
            }

            head_.store(node_pool_[0].get());
            tail_.store(node_pool_[0].get());
        }

        template<typename T>
        bool ThreadSafeQueue<T>::push(const T &item) {
            if (size_.load() >= capacity_) {
                return false;
            }

            size_t pool_idx = pool_index_.fetch_add(1) % node_pool_.size();
            Node *new_node = node_pool_[pool_idx].get();

            new_node->data.store(new T(item));
            new_node->next.store(nullptr);

            Node *prev_tail = tail_.exchange(new_node);
            prev_tail->next.store(new_node);

            size_.fetch_add(1);
            return true;
        }

        template<typename T>
        bool ThreadSafeQueue<T>::push(T &&item) {
            if (size_.load() >= capacity_) {
                return false;
            }

            size_t pool_idx = pool_index_.fetch_add(1) % node_pool_.size();
            Node *new_node = node_pool_[pool_idx].get();

            new_node->data.store(new T(std::move(item)));
            new_node->next.store(nullptr);

            Node *prev_tail = tail_.exchange(new_node);
            prev_tail->next.store(new_node);

            size_.fetch_add(1);
            return true;
        }

        template<typename T>
        bool ThreadSafeQueue<T>::pop(T &item) {
            Node *head = head_.load();
            Node *next = head->next.load();

            if (next == nullptr) {
                return false;
            }

            T *data = next->data.load();
            if (data == nullptr) {
                return false;
            }

            item = *data;
            delete data;
            next->data.store(nullptr);

            head_.store(next);
            size_.fetch_sub(1);

            return true;
        }

        template<typename T>
        size_t ThreadSafeQueue<T>::size() const {
            return size_.load();
        }

        template<typename T>
        bool ThreadSafeQueue<T>::empty() const {
            return size_.load() == 0;
        }

        template<typename T>
        bool ThreadSafeQueue<T>::full() const {
            return size_.load() >= capacity_;
        }

        template<typename T>
        void ThreadSafeQueue<T>::clear() {
            T item;
            while (pop(item)) {

            }
        }

    }
} 
