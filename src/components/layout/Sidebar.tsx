import { useState } from 'react';import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faMugHot, faBirthdayCake, faRobot, faGraduationCap } from '@fortawesome/free-solid-svg-icons';
import type { IconDefinition } from '@fortawesome/fontawesome-svg-core';

interface NavItem {
  icon: IconDefinition;
  hoverColor: string;
  path: string;
}

export default function Sidebar() {
  const [activeIndex, setActiveIndex] = useState(0);

  const navItems: NavItem[] = [
    { icon: faMugHot, hoverColor: 'hover:text-neon-cyan', path: '/' },
    { icon: faBirthdayCake, hoverColor: 'hover:text-neon-purple', path: '/' },
    { icon: faRobot, hoverColor: 'hover:text-neon-pink', path: '/' },
    { icon: faGraduationCap, hoverColor: 'hover:text-yellow-400', path: '/' }
  ];

  return (
    <div className="w-20 lg:w-24 bg-black border-r border-gray-800 flex flex-col items-center py-6 space-y-8" id="sidebar">
      <div className="text-neon-cyan text-2xl font-bold glow-text mb-4">N</div>
      <nav className="flex flex-col space-y-6">
        {navItems.map((item, index) => (
          <Link
            key={index}
            to={item.path}
            onClick={() => setActiveIndex(index)}
            className={`nav-item w-12 h-12 lg:w-14 lg:h-14 bg-gray-800 rounded-xl flex items-center justify-center hover:bg-gray-700 transition-all duration-300 ${
              activeIndex === index
                ? 'text-neon-cyan neon-border'
                : `text-gray-400 ${item.hoverColor}`
            }`}
          >
            <FontAwesomeIcon icon={item.icon} className="text-lg lg:text-xl" />
          </Link>
        ))}
      </nav>
    </div>
  );
}
