import React from 'react';
import {
  WrappedOrb,
  WrappedRing,
  WrappedDotField,
  WrappedSpark,
} from './WrappedDecorations';

/**
 * Chapter moods mapped to the Nivara Wrapped palette:
 * - 'intro': Deep Indigo (#17134F) + Electric Violet (#7C3AED)
 * - 'overview': Warm Cream (#FFF8EC) + Electric Violet (#7C3AED)
 * - 'spending': Warm Cream (#FFF8EC) + Warm Coral (#FF6B6B)
 * - 'category': Warm Cream (#FFF8EC) + Sunny Yellow (#FFD166) + Violet
 * - 'merchants': Deep Indigo (#17134F) + Fresh Mint (#6EE7B7)
 * - 'highlights': Warm Cream (#FFF8EC) + Soft Sky (#7DD3FC) + Violet
 * - 'closing': Deep Indigo (#17134F) + Multicolor Celebration Accents
 */

export function WrappedBackground({ mood = 'intro' }) {
  if (mood === 'intro') {
    return (
      <div className="absolute inset-0 overflow-hidden pointer-events-none z-0 bg-[#FDEAA8]">
        {/* Soft radial glow fields: Cream & Golden Yellow */}
        <WrappedOrb
          color="#FFF8E7"
          size={320}
          opacity={0.65}
          blur={50}
          className="-top-20 -right-20"
        />
        <WrappedOrb
          color="#F6DE8E"
          size={240}
          opacity={0.45}
          blur={45}
          className="-bottom-16 -left-16"
        />
        <WrappedOrb
          color="#FFF8E7"
          size={160}
          opacity={0.5}
          blur={35}
          className="top-1/2 -right-10"
        />

        {/* Delicate geometric elements in Charcoal & Cream */}
        <WrappedRing
          size={160}
          color="#24221F"
          opacity={0.12}
          strokeWidth={1.5}
          className="-top-12 -right-12"
        />
        <WrappedRing
          size={90}
          color="#F6DE8E"
          opacity={0.4}
          strokeWidth={1.5}
          className="top-1/3 -left-6"
        />

        {/* Small sparks / stars */}
        <WrappedSpark
          size={18}
          color="#24221F"
          opacity={0.45}
          className="top-24 right-10"
        />
        <WrappedSpark
          size={14}
          color="#FFF8E7"
          opacity={0.9}
          className="bottom-32 left-8"
        />
        <WrappedSpark
          size={12}
          color="#24221F"
          opacity={0.35}
          className="top-12 left-16"
        />

        {/* Dot field */}
        <WrappedDotField
          rows={3}
          cols={4}
          color="#24221F"
          opacity={0.15}
          className="bottom-20 right-6"
        />
      </div>
    );
  }

  if (mood === 'overview') {
    return (
      <div className="absolute inset-0 overflow-hidden pointer-events-none z-0 bg-[#FFF8EC]">
        {/* Violet ambient glow */}
        <WrappedOrb
          color="#7C3AED"
          size={280}
          opacity={0.14}
          blur={50}
          className="-top-16 -right-16"
        />
        <WrappedOrb
          color="#6EE7B7"
          size={180}
          opacity={0.18}
          blur={45}
          className="-bottom-10 -left-10"
        />

        {/* Concentric rings around center */}
        <WrappedRing
          size={220}
          color="#7C3AED"
          opacity={0.12}
          strokeWidth={1.5}
          className="top-28 -right-12"
        />

        <WrappedSpark
          size={14}
          color="#7C3AED"
          opacity={0.4}
          className="top-16 left-10"
        />
        <WrappedDotField
          rows={3}
          cols={3}
          color="#A855F7"
          opacity={0.2}
          className="top-36 left-6"
        />
      </div>
    );
  }

  if (mood === 'spending') {
    return (
      <div className="absolute inset-0 overflow-hidden pointer-events-none z-0 bg-[#FFF8EC]">
        {/* Coral ambient fields */}
        <WrappedOrb
          color="#FF6B6B"
          size={260}
          opacity={0.16}
          blur={50}
          className="-top-12 -right-10"
        />
        <WrappedOrb
          color="#FFD166"
          size={200}
          opacity={0.2}
          blur={45}
          className="bottom-10 -left-10"
        />

        <WrappedRing
          size={140}
          color="#FF6B6B"
          opacity={0.15}
          strokeWidth={1.5}
          className="top-32 -left-10"
        />

        <WrappedSpark
          size={16}
          color="#FF6B6B"
          opacity={0.5}
          className="top-20 right-8"
        />
        <WrappedDotField
          rows={2}
          cols={5}
          color="#FF6B6B"
          opacity={0.25}
          className="bottom-24 right-8"
        />
      </div>
    );
  }

  if (mood === 'category') {
    return (
      <div className="absolute inset-0 overflow-hidden pointer-events-none z-0 bg-[#FFF8EC]">
        {/* Sunny Yellow + Violet fields */}
        <WrappedOrb
          color="#FFD166"
          size={270}
          opacity={0.24}
          blur={45}
          className="-top-12 -left-12"
        />
        <WrappedOrb
          color="#7C3AED"
          size={220}
          opacity={0.12}
          blur={50}
          className="-bottom-10 -right-10"
        />

        {/* Crown/Trophy celebration spark */}
        <WrappedSpark
          size={18}
          color="#FFD166"
          opacity={0.8}
          className="top-16 right-10"
        />
        <WrappedSpark
          size={13}
          color="#7C3AED"
          opacity={0.4}
          className="top-32 right-20"
        />

        <WrappedRing
          size={180}
          color="#FFD166"
          opacity={0.25}
          strokeWidth={1.5}
          className="bottom-20 -left-16"
        />
        <WrappedDotField
          rows={3}
          cols={3}
          color="#7C3AED"
          opacity={0.18}
          className="top-24 left-8"
        />
      </div>
    );
  }

  if (mood === 'merchants') {
    return (
      <div className="absolute inset-0 overflow-hidden pointer-events-none z-0 bg-[#FED7C3]">
        {/* Soft Apricot & Forest Green depth */}
        <WrappedOrb
          color="#FFE4D3"
          size={290}
          opacity={0.6}
          blur={50}
          className="-top-16 -right-14"
        />
        <WrappedOrb
          color="#174C3C"
          size={210}
          opacity={0.12}
          blur={50}
          className="-bottom-16 -left-14"
        />
        <WrappedOrb
          color="#FFF8E7"
          size={160}
          opacity={0.45}
          blur={40}
          className="top-1/2 -left-10"
        />

        {/* Orbit / Location radar rings */}
        <WrappedRing
          size={200}
          color="#174C3C"
          opacity={0.14}
          strokeWidth={1.5}
          className="top-28 -right-16"
        />
        <WrappedRing
          size={120}
          color="#FFF8E7"
          opacity={0.4}
          strokeWidth={1.5}
          className="top-36 -right-6"
        />

        <WrappedSpark
          size={16}
          color="#174C3C"
          opacity={0.65}
          className="top-20 left-10"
        />
        <WrappedSpark
          size={14}
          color="#FFF8E7"
          opacity={0.85}
          className="bottom-28 right-12"
        />

        <WrappedDotField
          rows={4}
          cols={3}
          color="#174C3C"
          opacity={0.18}
          className="bottom-16 left-6"
        />
      </div>
    );
  }

  if (mood === 'highlights') {
    return (
      <div className="absolute inset-0 overflow-hidden pointer-events-none z-0 bg-[#FFF8EC]">
        {/* Soft Sky + Violet fields */}
        <WrappedOrb
          color="#7DD3FC"
          size={260}
          opacity={0.22}
          blur={50}
          className="-top-10 -right-10"
        />
        <WrappedOrb
          color="#A855F7"
          size={220}
          opacity={0.14}
          blur={45}
          className="bottom-10 -left-10"
        />

        <WrappedRing
          size={150}
          color="#7DD3FC"
          opacity={0.25}
          strokeWidth={1.5}
          className="top-20 -left-12"
        />

        <WrappedSpark
          size={16}
          color="#7DD3FC"
          opacity={0.7}
          className="top-20 right-8"
        />
        <WrappedSpark
          size={12}
          color="#FF6B6B"
          opacity={0.5}
          className="bottom-36 right-8"
        />

        <WrappedDotField
          rows={3}
          cols={4}
          color="#7DD3FC"
          opacity={0.3}
          className="bottom-20 right-6"
        />
      </div>
    );
  }

  // Closing / Celebration Finale (Warm Off-White + Coral Red + Deep Charcoal + Butter Yellow)
  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none z-0 bg-[#FFF8E7]">
      {/* Warm celebration orbs */}
      <WrappedOrb
        color="#F05A47"
        size={280}
        opacity={0.15}
        blur={55}
        className="-top-16 -left-16"
      />
      <WrappedOrb
        color="#F8D66D"
        size={240}
        opacity={0.35}
        blur={50}
        className="-top-10 -right-10"
      />
      <WrappedOrb
        color="#F05A47"
        size={190}
        opacity={0.12}
        blur={45}
        className="-bottom-12 -right-10"
      />
      <WrappedOrb
        color="#FFFFFF"
        size={200}
        opacity={0.5}
        blur={40}
        className="-bottom-10 -left-10"
      />

      {/* Celebration Sparks: Coral & Butter Yellow */}
      <WrappedSpark
        size={20}
        color="#F05A47"
        opacity={0.8}
        className="top-20 left-8"
      />
      <WrappedSpark
        size={16}
        color="#F8D66D"
        opacity={0.9}
        className="top-32 right-10"
      />
      <WrappedSpark
        size={18}
        color="#F05A47"
        opacity={0.7}
        className="bottom-28 right-8"
      />
      <WrappedSpark
        size={14}
        color="#24221F"
        opacity={0.35}
        className="bottom-32 left-10"
      />

      <WrappedRing
        size={160}
        color="#F05A47"
        opacity={0.15}
        strokeWidth={1.5}
        className="top-1/4 -right-16"
      />
      <WrappedRing
        size={100}
        color="#F8D66D"
        opacity={0.35}
        strokeWidth={1.5}
        className="bottom-20 -left-10"
      />

      <WrappedDotField
        rows={3}
        cols={4}
        color="#F05A47"
        opacity={0.18}
        className="top-12 right-20"
      />
    </div>
  );
}

export default WrappedBackground;
