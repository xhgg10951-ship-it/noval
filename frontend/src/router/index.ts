import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import CreateStoryView from '@/views/CreateStoryView.vue'
import StoryListView from '@/views/StoryListView.vue'
import StoryDetailView from '@/views/StoryDetailView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/create',
      name: 'create-story',
      component: CreateStoryView,
    },
    {
      path: '/stories',
      name: 'story-list',
      component: StoryListView,
    },
    {
      path: '/stories/:id',
      name: 'story-detail',
      component: StoryDetailView,
      props: true,
    },
  ],
})

export default router
