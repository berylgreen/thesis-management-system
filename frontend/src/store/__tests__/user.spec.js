
import { describe, it, expect, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useUserStore } from '../user';

describe('Store: user', () => {
  beforeEach(() => {
    // Create a new Pinia instance for each test
    setActivePinia(createPinia());
    // Clear localStorage mock
    window.localStorage.clear();
  });

  it('initializes with values from localStorage if they exist', () => {
    // Arrange
    window.localStorage.setItem('token', 'initial-token');
    window.localStorage.setItem('userId', '123');
    window.localStorage.setItem('username', 'testuser');
    window.localStorage.setItem('role', 'student');

    // Act
    const userStore = useUserStore();

    // Assert
    expect(userStore.token).toBe('initial-token');
    expect(userStore.userId).toBe('123');
    expect(userStore.username).toBe('testuser');
    expect(userStore.role).toBe('student');
  });

  it('initializes with empty strings if localStorage is empty', () => {
    const userStore = useUserStore();
    expect(userStore.token).toBe('');
    expect(userStore.userId).toBe('');
    expect(userStore.username).toBe('');
    expect(userStore.role).toBe('');
  });

  describe('setUser', () => {
    it('updates the state and localStorage with provided user data', () => {
      // Arrange
      const userStore = useUserStore();
      const userData = {
        token: 'new-token',
        userId: '456',
        username: 'newuser',
        role: 'teacher',
      };

      // Act
      userStore.setUser(userData);

      // Assert state
      expect(userStore.token).toBe('new-token');
      expect(userStore.userId).toBe('456');
      expect(userStore.username).toBe('newuser');
      expect(userStore.role).toBe('teacher');

      // Assert localStorage
      expect(window.localStorage.getItem('token')).toBe('new-token');
      expect(window.localStorage.getItem('userId')).toBe('456');
      expect(window.localStorage.getItem('username')).toBe('newuser');
      expect(window.localStorage.getItem('role')).toBe('teacher');
    });
  });

  describe('logout', () => {
    it('clears the state and removes items from localStorage', () => {
      // Arrange
      const userStore = useUserStore();
      // Pre-fill state
      userStore.setUser({
        token: 'some-token',
        userId: '789',
        username: 'someuser',
        role: 'admin',
      });
       
      // Act
      userStore.logout();

      // Assert state
      expect(userStore.token).toBe('');
      expect(userStore.userId).toBe('');
      expect(userStore.username).toBe('');
      expect(userStore.role).toBe('');

      // Assert localStorage
      expect(window.localStorage.getItem('token')).toBeNull();
      expect(window.localStorage.getItem('userId')).toBeNull();
      expect(window.localStorage.getItem('username')).toBeNull();
      expect(window.localStorage.getItem('role')).toBeNull();
    });
  });
});
