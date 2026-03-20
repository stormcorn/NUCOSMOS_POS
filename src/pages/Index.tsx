import "@/styles/pos-system.css";
import CartPanel from "@/components/Index/CartPanel";
import ProductsGrid from "@/components/Index/ProductsGrid";
import Sidebar from "@/components/layout/Sidebar";

const Index = () => {
  return (
    <div className="bg-gray-900 text-white">

      <div className="flex h-screen">
      <Sidebar />
      <div className="flex-1 flex" id="main-content">
      <ProductsGrid />
      <CartPanel />
      </div>
      </div>
    </div>
  );
};

export default Index;
