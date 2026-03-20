import { useEffect, useRef, useState } from 'react';export default function ProductsGrid() {
  const [activeNav, setActiveNav] = useState<string>('');
  const productCardsRef = useRef<(HTMLDivElement | null)[]>([]);

  useEffect(() => {
    const navItems = document.querySelectorAll('.nav-item');
    
    const handleNavClick = (item: Element) => {
      navItems.forEach(nav => nav.classList.remove('active', 'neon-border'));
      item.classList.add('active', 'neon-border');
    };

    navItems.forEach(item => {
      const clickHandler = () => handleNavClick(item);
      item.addEventListener('click', clickHandler);
    });

    return () => {
      navItems.forEach(item => {
        item.removeEventListener('click', () => {});
      });
    };
  }, []);

  const handleProductClick = (index: number) => {
    const card = productCardsRef.current[index];
    if (card) {
      card.style.transform = 'scale(0.98)';
      setTimeout(() => {
        card.style.transform = 'scale(1)';
      }, 100);
    }
  };

  const products = [
    {
      name: 'Premium Oolong Tea',
      description: 'Traditional Chinese tea with complex flavor profile',
      price: '$8.50',
      image: 'https://storage.googleapis.com/uxpilot-auth.appspot.com/7e0150e6ec-98206c2f00cab418562e.png',
      alt: 'premium oolong tea in elegant glass cup with steam rising, dark aesthetic, professional product photography'
    },
    {
      name: 'Matcha Latte',
      description: 'Ceremonial grade matcha with steamed milk',
      price: '$6.75',
      image: 'https://storage.googleapis.com/uxpilot-auth.appspot.com/1da7848da4-c7df44b95c4f15047973.png',
      alt: 'matcha latte with latte art in modern cup, green tea foam, dark background, professional photography'
    },
    {
      name: 'Cold Brew Coffee',
      description: 'Smooth 24-hour cold extraction',
      price: '$5.25',
      image: 'https://storage.googleapis.com/uxpilot-auth.appspot.com/11eca1e4ed-ac29664b2b0ab2d3d31f.png',
      alt: 'cold brew coffee in tall glass with ice, dark roast, minimalist presentation, professional product shot'
    },
    {
      name: 'Taro Bubble Tea',
      description: 'Creamy taro with chewy tapioca pearls',
      price: '$7.00',
      image: 'https://storage.googleapis.com/uxpilot-auth.appspot.com/bd3a3cb743-68a3b89349fc1eb8fe45.png',
      alt: 'bubble tea with tapioca pearls in clear cup, colorful drink, modern aesthetic, professional photography'
    }
  ];

  return (
    <div className="flex-1 p-6 lg:p-8" id="products-section">
      <header className="mb-8">
        <h1 className="text-2xl lg:text-3xl font-bold text-neon-cyan glow-text mb-2">Tea &amp; Drinks</h1>
        <p className="text-gray-400">Select your favorite beverages</p>
      </header>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {products.map((product, index) => (
          <div
            key={index}
            ref={el => productCardsRef.current[index] = el}
            className="product-card bg-gray-800 rounded-2xl overflow-hidden hover:bg-gray-750 transition-all duration-300 cursor-pointer group"
            onClick={() => handleProductClick(index)}
          >
            <div className="h-48 overflow-hidden">
              <img
                alt={product.alt}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                src={product.image}
              />
            </div>
            <div className="p-4">
              <h3 className="font-semibold text-lg mb-2">{product.name}</h3>
              <p className="text-gray-400 text-sm mb-3">{product.description}</p>
              <div className="flex justify-between items-center">
                <span className="text-neon-cyan font-bold text-lg">{product.price}</span>
                <button className="bg-neon-cyan text-black px-3 py-1 rounded-lg text-sm font-medium hover:shadow-neon transition-all duration-300">
                  Add
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
