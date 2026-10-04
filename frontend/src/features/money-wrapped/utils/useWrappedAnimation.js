import { useEffect } from 'react';
import anime from 'animejs';

/**
 * Hook to trigger a coordinated, staggered entrance timeline using Anime.js
 * for the active Story screen elements.
 */
export function useWrappedAnimation(containerRef, deps = []) {
  useEffect(() => {
    if (!containerRef.current) return;

    const el = containerRef.current;

    // Select staggered targets inside the story screen
    const tag = el.querySelector('[data-animate="tag"]');
    const headline = el.querySelector('[data-animate="headline"]');
    const hero = el.querySelector('[data-animate="hero"]');
    const items = el.querySelectorAll('[data-animate="item"]');
    const footer = el.querySelector('[data-animate="footer"]');

    // Reset initial states
    if (tag) tag.style.opacity = '0';
    if (headline) headline.style.opacity = '0';
    if (hero) hero.style.opacity = '0';
    items.forEach((item) => (item.style.opacity = '0'));
    if (footer) footer.style.opacity = '0';

    const tl = anime.timeline({
      easing: 'easeOutQuart',
    });

    if (tag) {
      tl.add({
        targets: tag,
        opacity: [0, 1],
        translateY: [10, 0],
        duration: 350,
      });
    }

    if (headline) {
      tl.add(
        {
          targets: headline,
          opacity: [0, 1],
          translateY: [16, 0],
          duration: 450,
        },
        '-=200'
      );
    }

    if (hero) {
      tl.add(
        {
          targets: hero,
          opacity: [0, 1],
          scale: [0.94, 1],
          translateY: [10, 0],
          duration: 480,
          easing: 'easeOutBack',
        },
        '-=250'
      );
    }

    if (items && items.length > 0) {
      tl.add(
        {
          targets: items,
          opacity: [0, 1],
          translateY: [12, 0],
          delay: anime.stagger(60),
          duration: 400,
        },
        '-=200'
      );
    }

    if (footer) {
      tl.add(
        {
          targets: footer,
          opacity: [0, 1],
          duration: 300,
        },
        '-=150'
      );
    }

    return () => {
      tl.pause();
    };
  }, deps);
}

export default useWrappedAnimation;
