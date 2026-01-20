import React from 'react';
import { CourseCardData } from '../types';
import { MiniLogo } from './Icons';

interface CourseCardProps {
  data: CourseCardData;
}

export const CourseCard: React.FC<CourseCardProps> = ({ data }) => {
  return (
    <div className="relative shrink-0 w-[220px] h-[340px] rounded-2xl overflow-hidden snap-center group cursor-pointer shadow-lg transform transition-transform duration-300 hover:scale-[1.02]">
      {/* Background Image */}
      <img 
        src={data.imageUrl} 
        alt={data.title} 
        className="absolute inset-0 w-full h-full object-cover grayscale-[20%]"
      />
      
      {/* Gradient Overlay for text readability */}
      <div className="absolute inset-0 bg-gradient-to-b from-black/30 via-transparent to-black/80" />

      {/* Content */}
      <div className="absolute inset-0 p-5 flex flex-col justify-between">
        {/* Top Branding */}
        <MiniLogo />

        {/* Bottom Text */}
        <div className="flex flex-col gap-1">
          <h3 className="text-white text-xl font-bold leading-tight drop-shadow-md">
            {data.title}
          </h3>
          <p className="text-gray-300 text-xs font-medium uppercase tracking-wider mt-2">
            {data.subtitle}
          </p>
        </div>
      </div>
    </div>
  );
};